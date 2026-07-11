package ba.autovendor.backend.woo.store;

import ba.autovendor.backend.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "woo_stores")
@Getter
public class WooStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "store_url", nullable = false)
    private String storeUrl;

    @Column(name = "encrypted_api_key", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String apiKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected WooStore() {
    }

    public WooStore(Long userId, String name, String storeUrl, String apiKey) {
        this.userId = userId;
        this.name = name;
        this.storeUrl = storeUrl;
        this.apiKey = apiKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStoreUrl(String storeUrl) {
        this.storeUrl = storeUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
