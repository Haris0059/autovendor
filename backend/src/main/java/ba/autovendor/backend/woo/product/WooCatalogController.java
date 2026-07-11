package ba.autovendor.backend.woo.product;

import ba.autovendor.backend.user.User;
import ba.autovendor.backend.woo.product.dto.WooAttributeResponse;
import ba.autovendor.backend.woo.product.dto.WooCategoryResponse;
import ba.autovendor.backend.woo.product.dto.WooProductResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/woo/stores/{storeId}")
public class WooCatalogController {

    private final WooCatalogService catalogService;

    public WooCatalogController(WooCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    public List<WooProductResponse> products(@AuthenticationPrincipal User user, @PathVariable Long storeId) {
        return catalogService.getProducts(user, storeId);
    }

    @GetMapping("/categories")
    public List<WooCategoryResponse> categories(@AuthenticationPrincipal User user, @PathVariable Long storeId) {
        return catalogService.getCategories(user, storeId);
    }

    @GetMapping("/attributes")
    public List<WooAttributeResponse> attributes(@AuthenticationPrincipal User user, @PathVariable Long storeId) {
        return catalogService.getAttributes(user, storeId);
    }
}
