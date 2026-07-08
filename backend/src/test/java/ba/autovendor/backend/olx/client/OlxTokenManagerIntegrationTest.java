package ba.autovendor.backend.olx.client;

import ba.autovendor.backend.TestcontainersConfiguration;
import ba.autovendor.backend.common.OlxAuthException;
import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.user.User;
import ba.autovendor.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OlxTokenManagerIntegrationTest {

    private static final String STORED_TOKEN = "163|stored-token";
    private static final String FRESH_TOKEN = "164|fresh-token";

    @Autowired
    private OlxTokenManager tokenManager;

    @Autowired
    private OlxAccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private OlxApiClient olxApiClient;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        user = userRepository.save(new User("tokens@test.ba", "irrelevant-hash", "Token Tester"));
    }

    @Test
    void validTokenIsReturnedWithoutLogin() {
        OlxAccount account = savedAccount(STORED_TOKEN, OffsetDateTime.now().plusDays(10));

        String token = tokenManager.getValidToken(account);

        assertThat(token).isEqualTo(STORED_TOKEN);
        verify(olxApiClient, never()).login(anyString(), anyString());
    }

    @Test
    void expiredTokenTriggersReLoginAndPersistsNewToken() {
        OlxAccount account = savedAccount(STORED_TOKEN, OffsetDateTime.now().minusHours(1));
        when(olxApiClient.login("olxuser", "olx-pass"))
                .thenReturn(new OlxLoginResult(FRESH_TOKEN, 555L));

        String token = tokenManager.getValidToken(account);

        assertThat(token).isEqualTo(FRESH_TOKEN);
        OlxAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getToken()).isEqualTo(FRESH_TOKEN);
        assertThat(reloaded.getTokenExpiresAt()).isAfter(OffsetDateTime.now().plusDays(29));
    }

    @Test
    void tokenNearExpiryIsRefreshed() {
        OlxAccount account = savedAccount(STORED_TOKEN, OffsetDateTime.now().plusMinutes(2));
        when(olxApiClient.login("olxuser", "olx-pass"))
                .thenReturn(new OlxLoginResult(FRESH_TOKEN, 555L));

        assertThat(tokenManager.getValidToken(account)).isEqualTo(FRESH_TOKEN);
    }

    @Test
    void withAccountTokenRetriesOnceAfterAuthRejection() {
        OlxAccount account = savedAccount(STORED_TOKEN, OffsetDateTime.now().plusDays(10));
        when(olxApiClient.login("olxuser", "olx-pass"))
                .thenReturn(new OlxLoginResult(FRESH_TOKEN, 555L));

        AtomicInteger calls = new AtomicInteger();
        String result = tokenManager.withAccountToken(account, token -> {
            if (calls.incrementAndGet() == 1) {
                throw new OlxAuthException("OLX rejected the account token");
            }
            return "ok:" + token;
        });

        assertThat(result).isEqualTo("ok:" + FRESH_TOKEN);
        assertThat(calls.get()).isEqualTo(2);
        verify(olxApiClient, times(1)).login(anyString(), anyString());
    }

    @Test
    void withAccountTokenPropagatesWhenRetryAlsoFails() {
        OlxAccount account = savedAccount(STORED_TOKEN, OffsetDateTime.now().plusDays(10));
        when(olxApiClient.login("olxuser", "olx-pass"))
                .thenReturn(new OlxLoginResult(FRESH_TOKEN, 555L));

        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> tokenManager.withAccountToken(account, token -> {
            calls.incrementAndGet();
            throw new OlxAuthException("OLX rejected the account token");
        })).isInstanceOf(OlxAuthException.class);

        assertThat(calls.get()).isEqualTo(2);
        verify(olxApiClient, times(1)).login(anyString(), anyString());
    }

    private OlxAccount savedAccount(String token, OffsetDateTime expiresAt) {
        OlxAccount account = new OlxAccount(user.getId(), "olxuser", "olx-pass", null);
        account.updateToken(token, 555L, expiresAt);
        return accountRepository.save(account);
    }
}
