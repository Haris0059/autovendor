package ba.autovendor.backend.olx.category.dto;

public record CategorySuggestionResponse(
        Long id,
        String name,
        Long count,
        String path
) {
}
