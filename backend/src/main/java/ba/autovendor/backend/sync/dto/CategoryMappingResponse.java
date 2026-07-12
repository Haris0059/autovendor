package ba.autovendor.backend.sync.dto;

public record CategoryMappingResponse(
        Long id,
        Long wooCategoryId,
        String wooCategoryName,
        Long olxCategoryId,
        String olxCategoryName
) {
}
