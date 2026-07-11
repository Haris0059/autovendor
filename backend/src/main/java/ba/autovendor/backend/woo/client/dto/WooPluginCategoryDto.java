package ba.autovendor.backend.woo.client.dto;

import java.util.List;

/**
 * Category node from the plugin. {@code /categories} returns a nested tree
 * (children populated, no parent field); categories embedded in a product
 * are flat {@code {id, name, slug}} (children null).
 */
public record WooPluginCategoryDto(
        Long id,
        String name,
        String slug,
        List<WooPluginCategoryDto> children
) {
}
