package ba.autovendor.backend.auth.dto;

import ba.autovendor.backend.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
}
