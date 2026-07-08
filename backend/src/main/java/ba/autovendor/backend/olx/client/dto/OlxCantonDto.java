package ba.autovendor.backend.olx.client.dto;

import java.util.List;

public record OlxCantonDto(Long id, String name, List<OlxCityDto> cities) {
}
