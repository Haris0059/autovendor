package ba.autovendor.backend.olx.client.dto;

import java.util.List;

public record OlxListingDto(
        Long id,
        String title,
        Double price,
        Boolean available,
        Long categoryId,
        Long cityId,
        List<OlxListingCityDto> cities,
        String listingType,
        String state,
        String status,
        Long brandId,
        Long modelId,
        String shortDescription,
        String skuNumber,
        OlxListingAdditionalDto additional,
        List<String> images,
        Long createdAt,
        Long date
) {
    public record OlxListingCityDto(Long id, String name) {
    }

    public record OlxListingAdditionalDto(String description) {
    }
}
