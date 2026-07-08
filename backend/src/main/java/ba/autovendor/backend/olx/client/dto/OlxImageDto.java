package ba.autovendor.backend.olx.client.dto;

import java.util.Map;

public record OlxImageDto(Long id, String name, Map<String, String> sizes, Boolean main) {
}
