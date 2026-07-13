package ba.autovendor.backend.olx.sponsor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "discounts")
@Getter
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "olx_account_id", nullable = false)
    private Long olxAccountId;

    @Column(name = "olx_listing_id", nullable = false)
    private Long olxListingId;

    @Column(name = "original_price", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "discount_price", nullable = false)
    private BigDecimal discountPrice;

    @Column(name = "days", nullable = false)
    private Integer days;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected Discount() {
    }

    public Discount(Long userId, Long olxAccountId, Long olxListingId,
                    BigDecimal originalPrice, BigDecimal discountPrice, Integer days,
                    OffsetDateTime startedAt, OffsetDateTime endsAt) {
        this.userId = userId;
        this.olxAccountId = olxAccountId;
        this.olxListingId = olxListingId;
        this.originalPrice = originalPrice;
        this.discountPrice = discountPrice;
        this.days = days;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public void end(OffsetDateTime at) {
        this.endedAt = at;
    }
}
