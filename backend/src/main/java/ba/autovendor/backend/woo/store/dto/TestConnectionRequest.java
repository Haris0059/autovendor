package ba.autovendor.backend.woo.store.dto;

import jakarta.validation.constraints.NotBlank;

public record TestConnectionRequest(
        @NotBlank String storeUrl,
        @NotBlank String apiKey
) {
}
