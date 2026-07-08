package ba.autovendor.backend.olx.listing;

import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.olx.listing.dto.ListingResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class ListingMapper {
    private ListingMapper() {
    }

    public static ListingResponse toResponse(OlxListingDto dto, Long accountId) {
        return toResponse(dto, accountId, null);
    }

    // statusOverride: OLX's /hidden endpoint returns items whose status field
    // still says "active" — the caller forces the status it queried for.
    public static ListingResponse toResponse(OlxListingDto dto, Long accountId, String statusOverride) {
        return new ListingResponse(
                dto.id(),
                accountId,
                dto.title(),
                description(dto),
                dto.price(),
                cityId(dto),
                dto.categoryId(),
                dto.listingType(),
                dto.state(),
                statusOverride != null ? statusOverride : dto.status(),
                images(dto),
                toDateTime(dto.createdAt() != null ? dto.createdAt() : dto.date()),
                toDateTime(dto.date())
        );
    }

    private static String description(OlxListingDto dto) {
        return dto.additional() != null ? dto.additional().description() : null;
    }

    private static Long cityId(OlxListingDto dto) {
        if (dto.cityId() != null) {
            return dto.cityId();
        }
        return dto.cities() != null && !dto.cities().isEmpty() ? dto.cities().getFirst().id() : null;
    }

    // OLX listing responses carry image URLs only (no ids) — real image ids exist
    // only in the image-upload response. Index-based ids keep the frontend shape.
    // Some list endpoints (e.g. finished) return images: null but still provide
    // the single `image` thumbnail — fall back to it.
    private static List<ListingResponse.ListingImageResponse> images(OlxListingDto dto) {
        List<ListingResponse.ListingImageResponse> images = new ArrayList<>();
        if (dto.images() == null || dto.images().isEmpty()) {
            if (dto.image() != null) {
                images.add(new ListingResponse.ListingImageResponse(0L, dto.image(), true));
            }
            return images;
        }
        for (int i = 0; i < dto.images().size(); i++) {
            images.add(new ListingResponse.ListingImageResponse((long) i, dto.images().get(i), i == 0));
        }
        return images;
    }

    private static OffsetDateTime toDateTime(Long epochSeconds) {
        return epochSeconds != null
                ? OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC)
                : null;
    }
}
