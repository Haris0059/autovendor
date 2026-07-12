package ba.autovendor.backend.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "product_links")
@Getter
public class ProductLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "olx_account_id", nullable = false)
    private Long olxAccountId;

    @Column(name = "woo_store_id", nullable = false)
    private Long wooStoreId;

    @Column(name = "olx_listing_id")
    private Long olxListingId;

    @Column(name = "woo_product_id", nullable = false)
    private Long wooProductId;

    @Column(name = "sync_direction", nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncDirection syncDirection;

    @Column(name = "woo_hash")
    private String wooHash;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected ProductLink() {
    }

    public ProductLink(Long userId, Long olxAccountId, Long wooStoreId,
                       Long olxListingId, Long wooProductId, SyncDirection syncDirection) {
        this.userId = userId;
        this.olxAccountId = olxAccountId;
        this.wooStoreId = wooStoreId;
        this.olxListingId = olxListingId;
        this.wooProductId = wooProductId;
        this.syncDirection = syncDirection;
    }

    public void setOlxListingId(Long olxListingId) {
        this.olxListingId = olxListingId;
    }

    public void markSynced(String wooHash, OffsetDateTime at) {
        this.wooHash = wooHash;
        this.lastSyncedAt = at;
    }
}
