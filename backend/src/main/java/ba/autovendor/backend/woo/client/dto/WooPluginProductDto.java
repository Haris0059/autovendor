package ba.autovendor.backend.woo.client.dto;

import java.util.List;

/** Product as served by the plugin's /catalog; only the fields we consume are bound. */
public record WooPluginProductDto(
        Long id,
        String hash,
        String name,
        String slug,
        String sku,
        String status,
        String price,
        String regularPrice,
        String salePrice,
        String stockStatus,
        Integer stockQty,
        List<WooPluginCategoryDto> categories,
        List<WooPluginImageDto> images,
        String description,
        String shortDescription,
        List<WooProductAttributeDto> attributes
) {
}
