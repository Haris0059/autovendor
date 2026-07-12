package ba.autovendor.backend.olx.category;

import ba.autovendor.backend.olx.category.dto.AttributeResponse;
import ba.autovendor.backend.olx.category.dto.CategoryResponse;
import ba.autovendor.backend.olx.category.dto.NamedResponse;
import ba.autovendor.backend.olx.client.dto.OlxAttributeDto;
import ba.autovendor.backend.olx.client.dto.OlxCategoryDto;
import ba.autovendor.backend.olx.client.dto.OlxNamedDto;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(OlxCategoryDto dto) {
        return new CategoryResponse(dto.id(), dto.name(), dto.slug(), dto.parentId());
    }

    public static AttributeResponse toResponse(OlxAttributeDto dto) {
        return new AttributeResponse(
                dto.id(),
                dto.type(),
                dto.name(),
                dto.inputType(),
                dto.displayName(),
                dto.options(),
                dto.required()
        );
    }

    public static NamedResponse toResponse(OlxNamedDto dto) {
        return new NamedResponse(dto.id(), dto.name(), dto.slug());
    }
}
