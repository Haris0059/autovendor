package ba.autovendor.backend.common;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        long total,
        int page,
        int perPage,
        int lastPage
) {
}
