package ba.autovendor.backend.olx.account;

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
@Table(name = "olx_accounts")
@Getter
public class OlxAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(name = "encrypted_password", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String password;

    @Column(name = "olx_user_id")
    private Long olxUserId;

    @Column(name = "default_city_id")
    private Long defaultCityId;

    @Column(name = "token_ciphertext")
    @Convert(converter = EncryptedStringConverter.class)
    private String token;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected OlxAccount() {
    }

    public OlxAccount(Long userId, String username, String password, Long defaultCityId) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.defaultCityId = defaultCityId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDefaultCityId(Long defaultCityId) {
        this.defaultCityId = defaultCityId;
    }

    public void updateToken(String token, Long olxUserId, OffsetDateTime tokenExpiresAt) {
        this.token = token;
        this.olxUserId = olxUserId;
        this.tokenExpiresAt = tokenExpiresAt;
    }
}
