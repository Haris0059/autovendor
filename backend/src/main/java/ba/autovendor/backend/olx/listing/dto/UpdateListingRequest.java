package ba.autovendor.backend.olx.listing.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record UpdateListingRequest(
        @Size(max = 255) String title,
        String shortDescription,
        String description,
        Double price,
        Long countryId,
        Long cityId,
        Long categoryId,
        Long brandId,
        Long modelId,
        String skuNumber,
        Boolean available,
        String listingType,
        String state,
        List<Map<String, Object>> attributes
) {
}
