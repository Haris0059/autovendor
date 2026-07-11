package ba.autovendor.backend.woo.store;

import ba.autovendor.backend.user.User;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooHashPageDto;
import ba.autovendor.backend.woo.store.dto.CreateWooStoreRequest;
import ba.autovendor.backend.woo.store.dto.TestConnectionResponse;
import ba.autovendor.backend.woo.store.dto.UpdateWooStoreRequest;
import ba.autovendor.backend.woo.store.dto.WooStoreResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreService {

    private static final int HASH_PAGE_SIZE = 200;
    // Guards against a broken plugin that keeps returning full pages (caps count at 10k products).
    private static final int MAX_HASH_PAGES = 50;

    private final WooStoreRepository storeRepository;
    private final WooPluginClient wooPluginClient;

    public StoreService(WooStoreRepository storeRepository, WooPluginClient wooPluginClient) {
        this.storeRepository = storeRepository;
        this.wooPluginClient = wooPluginClient;
    }

    public List<WooStoreResponse> list(User user) {
        return storeRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(WooStoreMapper::toResponse)
                .toList();
    }

    public WooStoreResponse get(User user, Long id) {
        return WooStoreMapper.toResponse(findOwned(user, id));
    }

    public TestConnectionResponse testConnection(String storeUrl, String apiKey) {
        String url = WooPluginClient.normalizeStoreUrl(storeUrl);
        return new TestConnectionResponse(true, countProducts(url, apiKey));
    }

    public TestConnectionResponse testStoredConnection(User user, Long id) {
        WooStore store = findOwned(user, id);
        return new TestConnectionResponse(true, countProducts(store.getStoreUrl(), store.getApiKey()));
    }

    @Transactional
    public WooStoreResponse create(User user, CreateWooStoreRequest request) {
        String storeUrl = WooPluginClient.normalizeStoreUrl(request.storeUrl());
        if (storeRepository.existsByUserIdAndStoreUrl(user.getId(), storeUrl)) {
            throw new IllegalArgumentException("Store already added");
        }
        // Validates URL + key against the plugin before anything is persisted.
        countProducts(storeUrl, request.apiKey());

        WooStore store = new WooStore(user.getId(), request.name(), storeUrl, request.apiKey());
        return WooStoreMapper.toResponse(storeRepository.save(store));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "woo-products", key = "#id"),
            @CacheEvict(cacheNames = "woo-categories", key = "#id"),
            @CacheEvict(cacheNames = "woo-attributes", key = "#id")
    })
    public WooStoreResponse update(User user, Long id, UpdateWooStoreRequest request) {
        WooStore store = findOwned(user, id);

        String normalizedUrl = request.storeUrl() != null
                ? WooPluginClient.normalizeStoreUrl(request.storeUrl())
                : store.getStoreUrl();
        boolean urlChanged = !normalizedUrl.equals(store.getStoreUrl());
        boolean keyChanged = request.apiKey() != null;

        if (urlChanged && storeRepository.existsByUserIdAndStoreUrl(user.getId(), normalizedUrl)) {
            throw new IllegalArgumentException("Store already added");
        }

        if (urlChanged || keyChanged) {
            String effectiveKey = keyChanged ? request.apiKey() : store.getApiKey();
            countProducts(normalizedUrl, effectiveKey);
            store.setStoreUrl(normalizedUrl);
            store.setApiKey(effectiveKey);
        }

        if (request.name() != null) {
            store.setName(request.name());
        }

        return WooStoreMapper.toResponse(storeRepository.save(store));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "woo-products", key = "#id"),
            @CacheEvict(cacheNames = "woo-categories", key = "#id"),
            @CacheEvict(cacheNames = "woo-attributes", key = "#id")
    })
    public void delete(User user, Long id) {
        storeRepository.delete(findOwned(user, id));
    }

    /**
     * Counts products by paging the plugin's lightweight /catalog-hashes.
     * Doubles as the connection test: a bad key or missing plugin throws before returning.
     */
    private int countProducts(String storeUrl, String apiKey) {
        int total = 0;
        for (int page = 1; page <= MAX_HASH_PAGES; page++) {
            WooHashPageDto result = wooPluginClient.getCatalogHashes(storeUrl, apiKey, page, HASH_PAGE_SIZE);
            int count = result != null && result.count() != null ? result.count() : 0;
            total += count;
            if (count < HASH_PAGE_SIZE) {
                break;
            }
        }
        return total;
    }

    private WooStore findOwned(User user, Long id) {
        return storeRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
    }
}
