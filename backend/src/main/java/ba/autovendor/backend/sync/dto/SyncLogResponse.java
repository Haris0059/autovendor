package ba.autovendor.backend.sync.dto;

import ba.autovendor.backend.sync.SyncStatus;

import java.time.OffsetDateTime;

public record SyncLogResponse(
        Long id,
        Long productLinkId,
        String action,
        SyncStatus status,
        String message,
        OffsetDateTime createdAt
) {
}
