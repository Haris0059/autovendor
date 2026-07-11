package ba.autovendor.backend.woo.store.dto;

import jakarta.validation.constraints.Size;

public record UpdateWooStoreRequest(
        @Size(max = 255) String name,
        @Size(max = 2048) String storeUrl,
        String apiKey
) {
}
