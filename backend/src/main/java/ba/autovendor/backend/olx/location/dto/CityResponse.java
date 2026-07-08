package ba.autovendor.backend.olx.location.dto;

public record CityResponse(
        Long id,
        String name,
        String zipCode,
        Double latitude,
        Double longitude,
        Long cantonId,
        Long stateId
) {
}
