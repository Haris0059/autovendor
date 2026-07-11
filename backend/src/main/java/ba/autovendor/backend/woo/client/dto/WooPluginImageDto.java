package ba.autovendor.backend.woo.client.dto;

public record WooPluginImageDto(
        Long id,
        String src,
        String name,
        String alt
) {
}
