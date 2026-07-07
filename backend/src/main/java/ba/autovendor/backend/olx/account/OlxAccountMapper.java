package ba.autovendor.backend.olx.account;

import ba.autovendor.backend.olx.account.dto.OlxAccountResponse;

public final class OlxAccountMapper {
    private OlxAccountMapper() {
    }

    public static OlxAccountResponse toResponse(OlxAccount account) {
        return new OlxAccountResponse(
                account.getId(),
                account.getUsername(),
                account.getOlxUserId(),
                account.getDefaultCityId(),
                account.getTokenExpiresAt(),
                account.getCreatedAt()
        );
    }
}
