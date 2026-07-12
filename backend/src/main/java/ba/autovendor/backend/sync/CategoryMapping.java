package ba.autovendor.backend.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "category_mappings")
@Getter
public class CategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "woo_category_id", nullable = false)
    private Long wooCategoryId;

    @Column(name = "woo_category_name", nullable = false)
    private String wooCategoryName;

    @Column(name = "olx_category_id", nullable = false)
    private Long olxCategoryId;

    @Column(name = "olx_category_name", nullable = false)
    private String olxCategoryName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected CategoryMapping() {
    }

    public CategoryMapping(Long userId, Long wooCategoryId, String wooCategoryName,
                           Long olxCategoryId, String olxCategoryName) {
        this.userId = userId;
        this.wooCategoryId = wooCategoryId;
        this.wooCategoryName = wooCategoryName;
        this.olxCategoryId = olxCategoryId;
        this.olxCategoryName = olxCategoryName;
    }
}
