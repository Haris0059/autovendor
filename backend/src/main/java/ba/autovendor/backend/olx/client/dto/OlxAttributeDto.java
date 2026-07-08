package ba.autovendor.backend.olx.client.dto;

import java.util.List;

public record OlxAttributeDto(
        Long id,
        String type,
        String name,
        String inputType,
        String displayName,
        List<String> options,
        Boolean required
) {
}
