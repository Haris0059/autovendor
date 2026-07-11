package ba.autovendor.backend.woo.product.dto;

/** {@code count} is not available from the plugin — always null; the frontend marks it optional. */
public record WooCategoryResponse(
        Long id,
        String name,
        String slug,
        long parent,
        Integer count
) {
}
