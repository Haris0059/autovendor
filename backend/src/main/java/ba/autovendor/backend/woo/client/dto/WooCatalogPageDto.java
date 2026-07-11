package ba.autovendor.backend.woo.client.dto;

import java.util.List;

/**
 * One /catalog page. The plugin also repeats the store's full categories/tags/attributes
 * on every page — deliberately not bound here.
 */
public record WooCatalogPageDto(
        List<WooPluginProductDto> products,
        Integer page,
        Integer perPage,
        Integer count
) {
}
