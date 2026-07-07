package ba.autovendor.backend.olx.account.dto;

import jakarta.validation.constraints.Size;

public record UpdateOlxAccountRequest(
        @Size(max = 255) String username,
        String password,
        Long defaultCityId
) {
}
