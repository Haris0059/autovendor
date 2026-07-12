package ba.autovendor.backend.webhook;

import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.sync.BulkSyncService;
import ba.autovendor.backend.sync.ProductLink;
import ba.autovendor.backend.sync.ProductLinkRepository;
import ba.autovendor.backend.sync.SyncEngine;
import ba.autovendor.backend.sync.SyncLog;
import ba.autovendor.backend.sync.SyncLogRepository;
import ba.autovendor.backend.sync.SyncStatus;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
import ba.autovendor.backend.woo.store.WooStore;
import ba.autovendor.backend.woo.store.WooStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles the WP plugin's fire-and-forget product events. Authentication is the
 * store's own API key: only callers who hold it (the plugin itself) can trigger
 * processing, and only for the stores it belongs to.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WooStoreRepository storeRepository;
    private final ProductLinkRepository linkRepository;
    private final OlxAccountRepository accountRepository;
    private final SyncLogRepository syncLogRepository;
    private final WooPluginClient wooPluginClient;
    private final SyncEngine syncEngine;
    private final BulkSyncService bulkSyncService;

    public WebhookService(WooStoreRepository storeRepository,
                          ProductLinkRepository linkRepository,
                          OlxAccountRepository accountRepository,
                          SyncLogRepository syncLogRepository,
                          WooPluginClient wooPluginClient,
                          SyncEngine syncEngine,
                          BulkSyncService bulkSyncService) {
        this.storeRepository = storeRepository;
        this.linkRepository = linkRepository;
        this.accountRepository = accountRepository;
        this.syncLogRepository = syncLogRepository;
        this.wooPluginClient = wooPluginClient;
        this.syncEngine = syncEngine;
        this.bulkSyncService = bulkSyncService;
    }

    /**
     * Stores whose URL matches the event's site AND whose stored key matches the
     * presented one. Empty means the caller is not authenticated for any tenant.
     * The same store URL may be registered by several users — all of them match.
     */
    public List<WooStore> authenticate(String siteUrl, String presentedKey) {
        if (presentedKey == null || presentedKey.isBlank()) {
            return List.of();
        }
        String normalized = WooPluginClient.normalizeStoreUrl(siteUrl);
        return storeRepository.findAllByStoreUrl(normalized).stream()
                .filter(store -> keysMatch(store.getApiKey(), presentedKey))
                .toList();
    }

    public int process(List<WooStore> stores, WebhookEventRequest event) {
        int processed = 0;
        for (WooStore store : stores) {
            try {
                handleForStore(store, event);
                processed++;
            } catch (Exception e) {
                log.warn("Webhook {} for product {} failed on store {}: {}",
                        event.event(), event.productId(), store.getId(), e.getMessage());
            }
        }
        return processed;
    }

    private void handleForStore(WooStore store, WebhookEventRequest event) {
        switch (event.event()) {
            case "product.created", "product.updated" -> upsert(store, event.productId());
            case "product.deleted" -> hide(store, event.productId());
            default -> log.warn("Ignoring unknown webhook event '{}' from {}", event.event(), store.getStoreUrl());
        }
    }

    private void upsert(WooStore store, long productId) {
        Optional<ProductLink> existing =
                linkRepository.findByWooStoreIdAndWooProductId(store.getId(), productId);
        WooPluginProductDto product =
                wooPluginClient.getProduct(store.getStoreUrl(), store.getApiKey(), productId);

        if (existing.isPresent()) {
            ProductLink link = existing.get();
            // Hash gate: WP fires the same hook several times per save — only the
            // first event with actual changes does work.
            if (product.hash() != null && product.hash().equals(link.getWooHash())) {
                return;
            }
            OlxAccount account = accountRepository.findById(link.getOlxAccountId()).orElseThrow();
            SyncEngine.SyncOutcome outcome = syncEngine.sync(link, account, product);
            syncLogRepository.save(new SyncLog(link.getUserId(), link.getId(), link.getOlxAccountId(),
                    link.getWooStoreId(), outcome.action(), outcome.status(), outcome.message()));
            bulkSyncService.evictListingsCache(Set.of(link.getUserId()));
            return;
        }

        OlxAccount account = bulkSyncService.resolveSoleAccount(store.getUserId());
        Optional<String> blocker = bulkSyncService.autoLinkBlocker(store, account, product);
        if (blocker.isPresent()) {
            // One-shot event, so unlike the sweep this is logged — without a link row.
            syncLogRepository.save(new SyncLog(store.getUserId(), null,
                    account != null ? account.getId() : null, store.getId(),
                    "create", SyncStatus.skipped, blocker.get()));
            return;
        }
        bulkSyncService.autoLinkAndSync(store, account, product);
        bulkSyncService.evictListingsCache(Set.of(store.getUserId()));
    }

    private void hide(WooStore store, long productId) {
        Optional<ProductLink> existing =
                linkRepository.findByWooStoreIdAndWooProductId(store.getId(), productId);
        if (existing.isEmpty()) {
            return;
        }
        ProductLink link = existing.get();
        OlxAccount account = accountRepository.findById(link.getOlxAccountId()).orElseThrow();
        SyncEngine.SyncOutcome outcome = syncEngine.hide(link, account);
        syncLogRepository.save(new SyncLog(link.getUserId(), link.getId(), link.getOlxAccountId(),
                link.getWooStoreId(), outcome.action(), outcome.status(), outcome.message()));
        bulkSyncService.evictListingsCache(Set.of(link.getUserId()));
    }

    private static boolean keysMatch(String stored, String presented) {
        if (stored == null) {
            return false;
        }
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }
}
