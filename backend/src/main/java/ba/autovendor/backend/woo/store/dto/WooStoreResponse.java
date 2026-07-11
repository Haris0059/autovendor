package ba.autovendor.backend.woo.store.dto;

import java.time.OffsetDateTime;

/** Deliberately has no api-key field, so the stored key can never leak into a response. */
public record WooStoreResponse(
        Long id,
        String name,
        String storeUrl,
        OffsetDateTime createdAt
) {
}
