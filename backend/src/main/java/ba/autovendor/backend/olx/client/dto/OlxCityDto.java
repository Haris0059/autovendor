package ba.autovendor.backend.olx.client.dto;

public record OlxCityDto(Long id, String name, OlxLocationDto location, Long cantonId) {
}
