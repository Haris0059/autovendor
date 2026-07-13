package ba.autovendor.backend.olx.client.dto;

/** GET /listing/refresh/limits — flat response, no data envelope (pinned live July 2026). */
public record OlxRefreshLimitsDto(
        Integer freeLimit,
        Integer freeCount,
        Integer paidCount,
        Integer listingCount
) {
}
