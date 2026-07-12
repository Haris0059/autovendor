package ba.autovendor.backend.sync.dto;

import java.util.Map;

public record CategoryMappingResponse(
        Long id,
        Long wooCategoryId,
        String wooCategoryName,
        Long olxCategoryId,
        String olxCategoryName,
        Map<String, String> attributeDefaults
) {
}
