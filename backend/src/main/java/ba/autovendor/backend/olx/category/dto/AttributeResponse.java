package ba.autovendor.backend.olx.category.dto;

import java.util.List;

public record AttributeResponse(
        Long id,
        String type,
        String name,
        String inputType,
        String displayName,
        List<String> options,
        Boolean required
) {
}
