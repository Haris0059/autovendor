package ba.autovendor.backend.woo.client.dto;

import java.util.List;

/** Note: the plugin puts the attribute label in {@code name} and the raw name in {@code slug}. */
public record WooPluginAttributeDto(
        Long id,
        String name,
        String slug,
        String type,
        String orderBy,
        List<Term> terms
) {
    public record Term(Long id, String name, String slug) {
    }
}
