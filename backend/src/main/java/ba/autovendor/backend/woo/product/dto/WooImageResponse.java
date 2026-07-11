package ba.autovendor.backend.woo.product.dto;

public record WooImageResponse(
        Long id,
        String src,
        String name,
        String alt
) {
}
