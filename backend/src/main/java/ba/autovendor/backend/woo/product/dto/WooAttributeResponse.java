package ba.autovendor.backend.woo.product.dto;

import java.util.List;

/**
 * {@code hasArchives}/{@code variation} aren't exposed by the plugin at this level — defaulted
 * to false. {@code options} stays null so the frontend falls back to rendering {@code terms}.
 */
public record WooAttributeResponse(
        Long id,
        String name,
        String slug,
        String type,
        String orderBy,
        boolean hasArchives,
        boolean variation,
        List<String> options,
        List<Term> terms
) {
    public record Term(Long id, String name, String slug) {
    }
}
