package ba.autovendor.backend.olx.sponsor.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateDiscountRequest(
        @NotNull BigDecimal originalPrice,
        @NotNull BigDecimal discountPrice,
        @NotNull Integer days
) {
}
