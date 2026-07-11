package ba.autovendor.backend.woo.product.dto;

import java.util.List;

public record WooProductResponse(
        Long id,
        String name,
        String slug,
        String sku,
        String status,
        String price,
        String regularPrice,
        String salePrice,
        String currency,
        String stockStatus,
        Integer stockQuantity,
        String description,
        String shortDescription,
        List<WooCategoryResponse> categories,
        List<WooImageResponse> images
) {
}
