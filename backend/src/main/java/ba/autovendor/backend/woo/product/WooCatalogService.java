package ba.autovendor.backend.woo.product;

import ba.autovendor.backend.user.User;
import ba.autovendor.backend.woo.product.dto.WooAttributeResponse;
import ba.autovendor.backend.woo.product.dto.WooCategoryResponse;
import ba.autovendor.backend.woo.product.dto.WooProductResponse;
import ba.autovendor.backend.woo.store.WooStore;
import ba.autovendor.backend.woo.store.WooStoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WooCatalogService {

    private final WooStoreRepository storeRepository;
    private final WooCatalogFetcher fetcher;

    public WooCatalogService(WooStoreRepository storeRepository, WooCatalogFetcher fetcher) {
        this.storeRepository = storeRepository;
        this.fetcher = fetcher;
    }

    public List<WooProductResponse> getProducts(User user, Long storeId) {
        return fetcher.fetchProducts(findOwned(user, storeId));
    }

    public List<WooCategoryResponse> getCategories(User user, Long storeId) {
        return fetcher.fetchCategories(findOwned(user, storeId));
    }

    public List<WooAttributeResponse> getAttributes(User user, Long storeId) {
        return fetcher.fetchAttributes(findOwned(user, storeId));
    }

    private WooStore findOwned(User user, Long storeId) {
        return storeRepository.findByIdAndUserId(storeId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Store not found"));
    }
}
