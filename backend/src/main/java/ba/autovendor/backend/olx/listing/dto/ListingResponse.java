package ba.autovendor.backend.olx.listing.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ListingResponse(
        Long id,
        Long accountId,
        String title,
        String description,
        Double price,
        Long cityId,
        Long categoryId,
        String listingType,
        String state,
        String status,
        List<ListingImageResponse> images,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record ListingImageResponse(Long id, String url, Boolean isMain) {
    }
}
