package ba.autovendor.backend.woo.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWooStoreRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2048) String storeUrl,
        @NotBlank String apiKey
) {
}
