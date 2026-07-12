package ba.autovendor.backend.sync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryMappingRequest(
        @NotNull Long wooCategoryId,
        @NotBlank @Size(max = 255) String wooCategoryName,
        @NotNull Long olxCategoryId,
        @NotBlank @Size(max = 255) String olxCategoryName
) {
}
