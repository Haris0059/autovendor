package ba.autovendor.backend.olx.category;

import ba.autovendor.backend.olx.category.dto.AttributeResponse;
import ba.autovendor.backend.olx.category.dto.CategoryResponse;
import ba.autovendor.backend.olx.category.dto.CategorySuggestionResponse;
import ba.autovendor.backend.olx.category.dto.NamedResponse;
import ba.autovendor.backend.olx.client.dto.OlxAttributeDto;
import ba.autovendor.backend.olx.client.dto.OlxCategoryDto;
import ba.autovendor.backend.olx.client.dto.OlxCategorySuggestionDto;
import ba.autovendor.backend.olx.client.dto.OlxNamedDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static CategorySuggestionResponse toResponse(OlxCategorySuggestionDto dto) {
        // OLX sends ancestors immediate-parent-first; display wants root-first.
        List<String> parents = new ArrayList<>(
                dto.parentCategories() == null ? List.<String>of() : dto.parentCategories());
        Collections.reverse(parents);
        return new CategorySuggestionResponse(dto.id(), dto.name(), dto.count(), String.join(" > ", parents));
    }
}
