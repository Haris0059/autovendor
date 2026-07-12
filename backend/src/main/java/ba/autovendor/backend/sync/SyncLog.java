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

/**
 * userId/olxAccountId/wooStoreId are denormalized copies from the link at write time:
 * productLinkId nulls out when the link is deleted (history is an audit trail), so
 * scoping and filtering must not depend on the join.
 */
@Entity
@Table(name = "sync_logs")
@Getter
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "product_link_id")
    private Long productLinkId;

    @Column(name = "olx_account_id")
    private Long olxAccountId;

    @Column(name = "woo_store_id")
    private Long wooStoreId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SyncStatus status;

    @Column
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected SyncLog() {
    }

    public SyncLog(Long userId, Long productLinkId, Long olxAccountId, Long wooStoreId,
                   String action, SyncStatus status, String message) {
        this.userId = userId;
        this.productLinkId = productLinkId;
        this.olxAccountId = olxAccountId;
        this.wooStoreId = wooStoreId;
        this.action = action;
        this.status = status;
        this.message = message;
    }
}
