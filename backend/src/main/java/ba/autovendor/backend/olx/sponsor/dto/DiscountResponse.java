package ba.autovendor.backend.olx.sponsor.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DiscountResponse(
        Long id,
        Long listingId,
        Long accountId,
        BigDecimal originalPrice,
        BigDecimal discountPrice,
        Integer days,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt
) {
}
