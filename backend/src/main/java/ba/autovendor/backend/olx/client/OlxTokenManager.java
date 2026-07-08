package ba.autovendor.backend.olx.client;

import ba.autovendor.backend.common.OlxAuthException;
import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.function.Function;

@Component
public class OlxTokenManager {

    private static final Duration REFRESH_SKEW = Duration.ofMinutes(5);

    private final OlxApiClient olxApiClient;
    private final OlxAccountRepository accountRepository;
    private final Duration tokenTtl;

    public OlxTokenManager(
            OlxApiClient olxApiClient,
            OlxAccountRepository accountRepository,
            @Value("${app.olx.token-ttl-days}") long tokenTtlDays
    ) {
        this.olxApiClient = olxApiClient;
        this.accountRepository = accountRepository;
        this.tokenTtl = Duration.ofDays(tokenTtlDays);
    }

    public String getValidToken(OlxAccount account) {
        if (account.getToken() == null
                || account.getTokenExpiresAt() == null
                || account.getTokenExpiresAt().isBefore(OffsetDateTime.now().plus(REFRESH_SKEW))) {
            return refresh(account);
        }
        return account.getToken();
    }

    public <T> T withAccountToken(OlxAccount account, Function<String, T> call) {
        try {
            return call.apply(getValidToken(account));
        } catch (OlxAuthException e) {
            // Stored token rejected before its assumed expiry — one re-login, one retry.
            return call.apply(refresh(account));
        }
    }

    private String refresh(OlxAccount account) {
        OlxLoginResult login = olxApiClient.login(account.getUsername(), account.getPassword());
        account.updateToken(login.token(), login.olxUserId(), OffsetDateTime.now().plus(tokenTtl));
        accountRepository.save(account);
        return login.token();
    }
}
