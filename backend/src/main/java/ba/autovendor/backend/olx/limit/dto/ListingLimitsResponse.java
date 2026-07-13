package ba.autovendor.backend.olx.limit.dto;

public record ListingLimitsResponse(
        CategoryLimit cars,
        CategoryLimit realEstate,
        CategoryLimit other
) {
    public record CategoryLimit(int used, int limit) {
    }
}
