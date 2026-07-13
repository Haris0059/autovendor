package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.olx.client.dto.OlxSponsorPriceDto;
import ba.autovendor.backend.olx.sponsor.dto.SponsorPriceResponse;
import ba.autovendor.backend.olx.sponsor.dto.SponsorshipResponse;

import java.util.Arrays;
import java.util.List;

public final class SponsorMapper {

    private SponsorMapper() {
    }

    public static SponsorshipResponse toResponse(Sponsorship sponsorship) {
        return new SponsorshipResponse(
                sponsorship.getId(),
                sponsorship.getOlxListingId(),
                sponsorship.getOlxAccountId(),
                sponsorship.getType(),
                sponsorship.getDays(),
                sponsorship.getRefreshEvery(),
                splitLocations(sponsorship.getLocations()),
                sponsorship.getStartedAt(),
                sponsorship.getEndsAt(),
                sponsorship.getPriceTotal()
        );
    }

    public static SponsorPriceResponse toPriceResponse(OlxSponsorPriceDto dto) {
        return new SponsorPriceResponse(dto.search(), dto.refresh(), dto.locations(), dto.extras(), dto.total());
    }

    static String joinLocations(List<String> locations) {
        return locations == null ? "" : String.join(",", locations);
    }

    static List<String> splitLocations(String locations) {
        if (locations == null || locations.isBlank()) {
            return List.of();
        }
        return Arrays.stream(locations.split(",")).filter(s -> !s.isBlank()).toList();
    }
}
