package ba.autovendor.backend.sync;

import ba.autovendor.backend.common.PageResponse;
import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.sync.dto.SyncLogResponse;
import ba.autovendor.backend.user.User;
import ba.autovendor.backend.woo.store.WooStore;
import ba.autovendor.backend.woo.store.WooStoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SyncService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductLinkRepository linkRepository;
    private final OlxAccountRepository accountRepository;
    private final WooStoreRepository storeRepository;
    private final SyncLogRepository syncLogRepository;
    private final SyncEngine syncEngine;

    public SyncService(ProductLinkRepository linkRepository,
                       OlxAccountRepository accountRepository,
                       WooStoreRepository storeRepository,
                       SyncLogRepository syncLogRepository,
                       SyncEngine syncEngine) {
        this.linkRepository = linkRepository;
        this.accountRepository = accountRepository;
        this.storeRepository = storeRepository;
        this.syncLogRepository = syncLogRepository;
        this.syncEngine = syncEngine;
    }

    /**
     * Not transactional on purpose: the engine performs network calls and its own
     * mini-writes; the log save below must survive a failed sync. Always returns the
     * written log (failed/skipped outcomes are data, not HTTP errors).
     */
    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public SyncLogResponse trigger(User user, Long productLinkId) {
        ProductLink link = linkRepository.findByIdAndUserId(productLinkId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Product link not found"));
        OlxAccount account = accountRepository.findById(link.getOlxAccountId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
        WooStore store = storeRepository.findById(link.getWooStoreId())
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));

        SyncEngine.SyncOutcome outcome = syncEngine.sync(link, account, store);

        SyncLog log = syncLogRepository.save(new SyncLog(user.getId(), link.getId(),
                link.getOlxAccountId(), link.getWooStoreId(),
                outcome.action(), outcome.status(), outcome.message()));
        return SyncMapper.toResponse(log);
    }

    public PageResponse<SyncLogResponse> history(User user, SyncStatus status, Long accountId,
                                                 Long storeId, int page, Integer perPage) {
        int size = perPage != null ? Math.clamp(perPage, 1, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        int safePage = Math.max(page, 1);

        Specification<SyncLog> spec = (root, query, cb) -> cb.equal(root.get("userId"), user.getId());
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (accountId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("olxAccountId"), accountId));
        }
        if (storeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("wooStoreId"), storeId));
        }

        Page<SyncLog> result = syncLogRepository.findAll(spec, PageRequest.of(safePage - 1, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));

        return new PageResponse<>(
                result.getContent().stream().map(SyncMapper::toResponse).toList(),
                result.getTotalElements(),
                safePage,
                size,
                Math.max(result.getTotalPages(), 1)
        );
    }
}
