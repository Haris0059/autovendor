package ba.autovendor.backend.sync;

import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.olx.client.OlxTokenManager;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooHashPageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
import ba.autovendor.backend.woo.store.WooStore;
import ba.autovendor.backend.woo.store.WooStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Hash-gated store sweep behind the scheduled job (and reused per-store by the
 * webhook path). Deliberately not transactional — same reasoning as
 * {@link SyncEngine}: it spans many network calls and every partial result
 * (created links, listing ids, logs) must survive later failures.
 */
@Service
public class BulkSyncService {

    private static final Logger log = LoggerFactory.getLogger(BulkSyncService.class);

    private static final int HASH_PAGE_SIZE = 200;
    private static final int MAX_HASH_PAGES = 50;
    private static final int CATALOG_BATCH_SIZE = 50;

    private final WooStoreRepository storeRepository;
    private final ProductLinkRepository linkRepository;
    private final OlxAccountRepository accountRepository;
    private final CategoryMappingRepository mappingRepository;
    private final SyncLogRepository syncLogRepository;
    private final WooPluginClient wooPluginClient;
    private final SyncEngine syncEngine;
    private final OlxTokenManager tokenManager;
    private final CacheManager cacheManager;

    public BulkSyncService(WooStoreRepository storeRepository,
                           ProductLinkRepository linkRepository,
                           OlxAccountRepository accountRepository,
                           CategoryMappingRepository mappingRepository,
                           SyncLogRepository syncLogRepository,
                           WooPluginClient wooPluginClient,
                           SyncEngine syncEngine,
                           OlxTokenManager tokenManager,
                           CacheManager cacheManager) {
        this.storeRepository = storeRepository;
        this.linkRepository = linkRepository;
        this.accountRepository = accountRepository;
        this.mappingRepository = mappingRepository;
        this.syncLogRepository = syncLogRepository;
        this.wooPluginClient = wooPluginClient;
        this.syncEngine = syncEngine;
        this.tokenManager = tokenManager;
        this.cacheManager = cacheManager;
    }

    /** Scheduled sweep over every store; one broken store must not stall the rest. */
    public void sweepAllStores() {
        for (WooStore store : storeRepository.findAll()) {
            try {
                syncStore(store);
            } catch (Exception e) {
                log.warn("Bulk sync failed for store {} ({}): {}", store.getId(), store.getStoreUrl(), e.getMessage());
            }
        }
    }

    /** Scheduled token upkeep: re-login accounts whose token expires within {@code window}. */
    public void refreshExpiringTokens(Duration window) {
        for (OlxAccount account : accountRepository.findAll()) {
            try {
                if (tokenManager.refreshIfExpiringWithin(account, window)) {
                    log.info("Refreshed OLX token for account {}", account.getId());
                }
            } catch (Exception e) {
                log.warn("Token refresh failed for OLX account {}: {}", account.getId(), e.getMessage());
            }
        }
    }

    /**
     * One store pass: fetch all product hashes, re-sync linked products whose hash
     * changed, and auto-link unlinked ones that can fully sync right now (publish
     * status, exactly one OLX account, mapped category, default city set).
     * Precondition misses stay silent — they would repeat every run.
     */
    public void syncStore(WooStore store) {
        Map<Long, String> currentHashes = fetchAllHashes(store);
        Map<Long, ProductLink> linksByProductId = new HashMap<>();
        for (ProductLink link : linkRepository.findAllByWooStoreId(store.getId())) {
            linksByProductId.put(link.getWooProductId(), link);
        }

        List<Long> toFetch = new ArrayList<>();
        for (Map.Entry<Long, String> entry : currentHashes.entrySet()) {
            ProductLink link = linksByProductId.get(entry.getKey());
            if (link == null || !entry.getValue().equals(link.getWooHash())) {
                toFetch.add(entry.getKey());
            }
        }

        Map<Long, WooPluginProductDto> products = fetchByIds(store, toFetch);
        OlxAccount autoLinkAccount = resolveSoleAccount(store.getUserId());
        Set<Long> affectedUsers = new HashSet<>();

        for (Long productId : toFetch) {
            WooPluginProductDto product = products.get(productId);
            if (product == null) {
                continue;
            }
            ProductLink link = linksByProductId.get(productId);
            if (link != null) {
                resync(link, product, affectedUsers);
            } else {
                autoLink(store, autoLinkAccount, product, affectedUsers);
            }
        }

        evictListingsCache(affectedUsers);
    }

    private void resync(ProductLink link, WooPluginProductDto product, Set<Long> affectedUsers) {
        OlxAccount account = accountRepository.findById(link.getOlxAccountId()).orElse(null);
        if (account == null) {
            log.warn("Link {} references missing OLX account {}", link.getId(), link.getOlxAccountId());
            return;
        }
        SyncEngine.SyncOutcome outcome = syncEngine.sync(link, account, product);
        logOutcome(link, outcome);
        affectedUsers.add(link.getUserId());
    }

    private void autoLink(WooStore store, OlxAccount account, WooPluginProductDto product, Set<Long> affectedUsers) {
        if (autoLinkBlocker(store, account, product).isPresent()) {
            return;
        }
        autoLinkAndSync(store, account, product);
        affectedUsers.add(store.getUserId());
    }

    /**
     * Why an unlinked product cannot be onboarded right now, or empty when it can.
     * The sweep stays silent about blockers (they repeat every run); the webhook
     * path logs them once per event.
     */
    public Optional<String> autoLinkBlocker(WooStore store, OlxAccount account, WooPluginProductDto product) {
        if (account == null) {
            return Optional.of("Auto-link needs exactly one OLX account on this user");
        }
        if (account.getDefaultCityId() == null) {
            return Optional.of("OLX account has no default city set");
        }
        if (!"publish".equals(product.status())) {
            return Optional.of("Product is not published in WooCommerce");
        }
        if (product.categories() == null || product.categories().isEmpty()) {
            return Optional.of("Product has no WooCommerce category to map");
        }
        var wooCategory = product.categories().getFirst();
        if (mappingRepository.findByUserIdAndWooCategoryId(store.getUserId(), wooCategory.id()).isEmpty()) {
            return Optional.of("No category mapping for Woo category '" + wooCategory.name()
                    + "' (id " + wooCategory.id() + ")");
        }
        return Optional.empty();
    }

    /** Creates the link, runs the engine's create path, and always logs (first onboarding). */
    public SyncEngine.SyncOutcome autoLinkAndSync(WooStore store, OlxAccount account, WooPluginProductDto product) {
        ProductLink link = linkRepository.save(new ProductLink(
                store.getUserId(), account.getId(), store.getId(),
                null, product.id(), SyncDirection.woo_to_olx));
        SyncEngine.SyncOutcome outcome = syncEngine.sync(link, account, product);
        syncLogRepository.save(newLog(link, outcome));
        return outcome;
    }

    /** Auto-linking needs an unambiguous target account: exactly one on the store's user. */
    public OlxAccount resolveSoleAccount(Long userId) {
        List<OlxAccount> accounts = accountRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return accounts.size() == 1 ? accounts.getFirst() : null;
    }

    /**
     * Failed/skipped outcomes recur every sweep until the product changes or the
     * user intervenes; suppress the repeat rows so history stays readable.
     * Successes always log (an identical message still means a distinct sync).
     */
    private void logOutcome(ProductLink link, SyncEngine.SyncOutcome outcome) {
        if (outcome.status() != SyncStatus.success) {
            Optional<SyncLog> last = syncLogRepository.findFirstByProductLinkIdOrderByIdDesc(link.getId());
            if (last.isPresent() && sameOutcome(last.get(), outcome)) {
                return;
            }
        }
        syncLogRepository.save(newLog(link, outcome));
    }

    private static boolean sameOutcome(SyncLog last, SyncEngine.SyncOutcome outcome) {
        return last.getAction().equals(outcome.action())
                && last.getStatus() == outcome.status()
                && java.util.Objects.equals(last.getMessage(), outcome.message());
    }

    private SyncLog newLog(ProductLink link, SyncEngine.SyncOutcome outcome) {
        return new SyncLog(link.getUserId(), link.getId(), link.getOlxAccountId(),
                link.getWooStoreId(), outcome.action(), outcome.status(), outcome.message());
    }

    private Map<Long, String> fetchAllHashes(WooStore store) {
        Map<Long, String> hashes = new LinkedHashMap<>();
        for (int page = 1; page <= MAX_HASH_PAGES; page++) {
            WooHashPageDto pageDto = wooPluginClient.getCatalogHashes(
                    store.getStoreUrl(), store.getApiKey(), page, HASH_PAGE_SIZE);
            if (pageDto.products() == null || pageDto.products().isEmpty()) {
                break;
            }
            for (WooHashPageDto.ProductHash hash : pageDto.products()) {
                hashes.put(hash.id(), hash.hash());
            }
            if (pageDto.products().size() < HASH_PAGE_SIZE) {
                break;
            }
        }
        return hashes;
    }

    private Map<Long, WooPluginProductDto> fetchByIds(WooStore store, List<Long> ids) {
        Map<Long, WooPluginProductDto> products = new HashMap<>();
        for (int from = 0; from < ids.size(); from += CATALOG_BATCH_SIZE) {
            List<Long> batch = ids.subList(from, Math.min(from + CATALOG_BATCH_SIZE, ids.size()));
            var pageDto = wooPluginClient.getCatalogByIds(store.getStoreUrl(), store.getApiKey(), batch);
            if (pageDto.products() != null) {
                for (WooPluginProductDto product : pageDto.products()) {
                    products.put(product.id(), product);
                }
            }
        }
        return products;
    }

    public void evictListingsCache(Set<Long> userIds) {
        Cache cache = cacheManager.getCache("olx-listings-all");
        if (cache == null) {
            return;
        }
        userIds.forEach(cache::evict);
    }
}
