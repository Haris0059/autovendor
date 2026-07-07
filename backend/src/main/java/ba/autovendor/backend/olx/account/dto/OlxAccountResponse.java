package ba.autovendor.backend.olx.account.dto;

import java.time.OffsetDateTime;

public record OlxAccountResponse(
        Long id,
        String username,
        Long olxUserId,
        Long defaultCityId,
        OffsetDateTime tokenExpiresAt,
        OffsetDateTime createdAt
) {
}
