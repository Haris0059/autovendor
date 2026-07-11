package ba.autovendor.backend.woo.product;

import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooCatalogPageDto;
import ba.autovendor.backend.woo.product.dto.WooAttributeResponse;
import ba.autovendor.backend.woo.product.dto.WooCategoryResponse;
import ba.autovendor.backend.woo.product.dto.WooProductResponse;
import ba.autovendor.backend.woo.store.WooStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Cached upstream aggregation, keyed by store id. Deliberately separate from
 * {@link WooCatalogService} so the per-request ownership check there is never
 * skipped by a cache hit — a hit here can only be reached after the caller
 * resolved the store through the user-scoped query.
 */
@Service
public class WooCatalogFetcher {

    private static final int CATALOG_PAGE_SIZE = 100;
    // Guards against a broken plugin that keeps returning full pages (caps at 10k products).
    private static final int MAX_CATALOG_PAGES = 100;

    private final WooPluginClient wooPluginClient;

    public WooCatalogFetcher(WooPluginClient wooPluginClient) {
        this.wooPluginClient = wooPluginClient;
    }

    @Cacheable(cacheNames = "woo-products", key = "#store.id")
    public List<WooProductResponse> fetchProducts(WooStore store) {
        List<WooProductResponse> products = new ArrayList<>();
        for (int page = 1; page <= MAX_CATALOG_PAGES; page++) {
            WooCatalogPageDto result = wooPluginClient.getCatalog(
                    store.getStoreUrl(), store.getApiKey(), page, CATALOG_PAGE_SIZE);
            if (result == null || result.products() == null || result.products().isEmpty()) {
                break;
            }
            result.products().forEach(product -> products.add(WooCatalogMapper.toResponse(product)));
            if (result.products().size() < CATALOG_PAGE_SIZE) {
                break;
            }
        }
        return products;
    }

    @Cacheable(cacheNames = "woo-categories", key = "#store.id")
    public List<WooCategoryResponse> fetchCategories(WooStore store) {
        return WooCatalogMapper.flattenTree(
                wooPluginClient.getCategories(store.getStoreUrl(), store.getApiKey()));
    }

    @Cacheable(cacheNames = "woo-attributes", key = "#store.id")
    public List<WooAttributeResponse> fetchAttributes(WooStore store) {
        List<WooAttributeResponse> attributes = new ArrayList<>();
        wooPluginClient.getAttributes(store.getStoreUrl(), store.getApiKey())
                .forEach(attribute -> attributes.add(WooCatalogMapper.toResponse(attribute)));
        return attributes;
    }
}
