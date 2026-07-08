package ba.autovendor.backend.olx.client.dto;

import java.util.List;

public record OlxStateDto(Long id, String name, String code, List<OlxCantonDto> cantons) {
}
