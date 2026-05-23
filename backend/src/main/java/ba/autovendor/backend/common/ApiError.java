package ba.autovendor.backend.common;

import java.util.List;

public record ApiError(String detail, List<String> errors) {
    public ApiError(String detail) { this(detail, null); }
}
