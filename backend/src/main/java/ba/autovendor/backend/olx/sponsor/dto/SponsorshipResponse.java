package ba.autovendor.backend.olx.sponsor.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SponsorshipResponse(
        Long id,
        Long listingId,
        Long accountId,
        Integer type,
        Integer days,
        Integer refreshEvery,
        List<String> locations,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        BigDecimal priceTotal
) {
}
