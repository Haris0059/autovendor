package ba.autovendor.backend.olx.client.dto;

import java.math.BigDecimal;

/**
 * Quote from GET /listings/{id}/sponsore/price. OLX returns extra fields
 * (total_without_discount, discount, discount_percentage) — ignored.
 */
public record OlxSponsorPriceDto(
        BigDecimal search,
        BigDecimal refresh,
        BigDecimal locations,
        BigDecimal extras,
        BigDecimal total
) {
}
