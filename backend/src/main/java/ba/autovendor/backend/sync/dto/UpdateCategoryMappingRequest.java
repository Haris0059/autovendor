package ba.autovendor.backend.sync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** The Woo category is the mapping's identity — retargeting the OLX side and defaults only. */
public record UpdateCategoryMappingRequest(
        @NotNull Long olxCategoryId,
        @NotBlank @Size(max = 255) String olxCategoryName,
        Map<String, String> attributeDefaults
) {
}
