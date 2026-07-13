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
@Table(name = "sponsorships")
@Getter
public class Sponsorship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "olx_account_id", nullable = false)
    private Long olxAccountId;

    @Column(name = "olx_listing_id", nullable = false)
    private Long olxListingId;

    @Column(name = "type", nullable = false)
    private Integer type;

    @Column(name = "days", nullable = false)
    private Integer days;

    @Column(name = "refresh_every", nullable = false)
    private Integer refreshEvery;

    @Column(name = "locations", nullable = false)
    private String locations;

    @Column(name = "price_total")
    private BigDecimal priceTotal;

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

    protected Sponsorship() {
    }

    public Sponsorship(Long userId, Long olxAccountId, Long olxListingId, Integer type,
                       Integer days, Integer refreshEvery, String locations, BigDecimal priceTotal,
                       OffsetDateTime startedAt, OffsetDateTime endsAt) {
        this.userId = userId;
        this.olxAccountId = olxAccountId;
        this.olxListingId = olxListingId;
        this.type = type;
        this.days = days;
        this.refreshEvery = refreshEvery;
        this.locations = locations;
        this.priceTotal = priceTotal;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public void end(OffsetDateTime at) {
        this.endedAt = at;
    }
}
