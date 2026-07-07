package ba.autovendor.backend.olx.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOlxAccountRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank String password,
        Long defaultCityId
) {
}
