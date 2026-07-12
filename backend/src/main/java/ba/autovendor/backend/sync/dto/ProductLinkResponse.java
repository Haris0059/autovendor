package ba.autovendor.backend.sync.dto;

import ba.autovendor.backend.sync.SyncDirection;

import java.time.OffsetDateTime;

public record ProductLinkResponse(
        Long id,
        Long olxAccountId,
        Long wooStoreId,
        Long olxListingId,
        Long wooProductId,
        SyncDirection syncDirection,
        OffsetDateTime lastSyncedAt
) {
}
