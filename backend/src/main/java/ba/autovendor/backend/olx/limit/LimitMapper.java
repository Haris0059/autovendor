package ba.autovendor.backend.olx.limit;

import ba.autovendor.backend.olx.client.dto.OlxRefreshLimitsDto;
import ba.autovendor.backend.olx.limit.dto.ListingLimitsResponse;
import ba.autovendor.backend.olx.limit.dto.ListingLimitsResponse.CategoryLimit;
import ba.autovendor.backend.olx.limit.dto.RefreshLimitsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

public final class LimitMapper {

    private static final Logger log = LoggerFactory.getLogger(LimitMapper.class);

    private LimitMapper() {
    }

    /**
     * Live shape (pinned July 2026, deviates from the docs):
     * {"data": {"cars"|"real-estate"|"car-parts"|"other": {"limit", "unlimited", "listings"}}}.
     * "car-parts" is not part of the frontend contract and is dropped; the
     * "unlimited" flag is ignored (limit is passed through as OLX reports it).
     * Missing keys map to zeros with a WARN — shape drift must never 500.
     */
    public static ListingLimitsResponse toListingLimits(JsonNode node) {
        JsonNode data = node != null && node.has("data") ? node.get("data") : node;
        return new ListingLimitsResponse(
                category(data, "cars"),
                category(data, "real-estate"),
                category(data, "other")
        );
    }

    private static CategoryLimit category(JsonNode data, String key) {
        JsonNode category = data == null ? null : data.get(key);
        if (category == null || !category.isObject()) {
            log.warn("OLX listing-limits response is missing the '{}' category: {}", key, data);
            return new CategoryLimit(0, 0);
        }
        return new CategoryLimit(
                category.path("listings").asInt(0),
                category.path("limit").asInt(0)
        );
    }

    public static RefreshLimitsResponse toRefreshLimits(OlxRefreshLimitsDto dto) {
        return new RefreshLimitsResponse(
                dto.freeLimit() != null ? dto.freeLimit() : 0,
                dto.freeCount() != null ? dto.freeCount() : 0,
                dto.paidCount() != null ? dto.paidCount() : 0,
                dto.listingCount() != null ? dto.listingCount() : 0
        );
    }
}
