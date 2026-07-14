package ba.autovendor.backend.olx.client.dto;

import java.util.List;

/**
 * Element of GET /categories/suggest?keyword= — OLX ranks categories by where
 * real listings matching the keyword live; count = matching listings,
 * parent_categories = ancestors, immediate parent first.
 */
public record OlxCategorySuggestionDto(
        Long id,
        Long count,
        String name,
        List<String> parentCategories
) {
}
