package ba.autovendor.backend.olx.sponsor.dto;

import java.math.BigDecimal;

public record SponsorPriceResponse(
        BigDecimal search,
        BigDecimal refresh,
        BigDecimal locations,
        BigDecimal extras,
        BigDecimal total
) {
}
