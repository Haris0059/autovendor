package ba.autovendor.backend.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

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

    /** OLX attribute name → exact option value; null on mappings created before step 9. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attribute_defaults")
    private Map<String, String> attributeDefaults;

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

    public void setAttributeDefaults(Map<String, String> attributeDefaults) {
        this.attributeDefaults = attributeDefaults;
    }

    public void updateOlxTarget(Long olxCategoryId, String olxCategoryName) {
        this.olxCategoryId = olxCategoryId;
        this.olxCategoryName = olxCategoryName;
    }
}
