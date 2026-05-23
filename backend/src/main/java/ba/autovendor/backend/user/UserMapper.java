package ba.autovendor.backend.user;

import ba.autovendor.backend.user.dto.UserResponse;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
