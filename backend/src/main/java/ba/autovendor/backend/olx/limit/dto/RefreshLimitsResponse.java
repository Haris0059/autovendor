package ba.autovendor.backend.olx.limit.dto;

public record RefreshLimitsResponse(
        int freeLimit,
        int freeCount,
        int paidCount,
        int listingCount
) {
}
