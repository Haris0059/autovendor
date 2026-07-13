package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.olx.sponsor.dto.DiscountResponse;

public final class DiscountMapper {

    private DiscountMapper() {
    }

    public static DiscountResponse toResponse(Discount discount) {
        return new DiscountResponse(
                discount.getId(),
                discount.getOlxListingId(),
                discount.getOlxAccountId(),
                discount.getOriginalPrice(),
                discount.getDiscountPrice(),
                discount.getDays(),
                discount.getStartedAt(),
                discount.getEndsAt()
        );
    }
}
