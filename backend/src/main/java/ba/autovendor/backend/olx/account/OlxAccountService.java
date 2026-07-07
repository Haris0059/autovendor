package ba.autovendor.backend.olx.account;

import ba.autovendor.backend.olx.account.dto.CreateOlxAccountRequest;
import ba.autovendor.backend.olx.account.dto.OlxAccountResponse;
import ba.autovendor.backend.olx.account.dto.UpdateOlxAccountRequest;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxLoginResult;
import ba.autovendor.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OlxAccountService {

    private final OlxAccountRepository accountRepository;
    private final OlxApiClient olxApiClient;
    private final Duration tokenTtl;

    public OlxAccountService(
            OlxAccountRepository accountRepository,
            OlxApiClient olxApiClient,
            @Value("${app.olx.token-ttl-days}") long tokenTtlDays
    ) {
        this.accountRepository = accountRepository;
        this.olxApiClient = olxApiClient;
        this.tokenTtl = Duration.ofDays(tokenTtlDays);
    }

    public List<OlxAccountResponse> list(User user) {
        return accountRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OlxAccountMapper::toResponse)
                .toList();
    }

    public OlxAccountResponse get(User user, Long id) {
        return OlxAccountMapper.toResponse(findOwned(user, id));
    }

    @Transactional
    public OlxAccountResponse create(User user, CreateOlxAccountRequest request) {
        if (accountRepository.existsByUserIdAndUsername(user.getId(), request.username())) {
            throw new IllegalArgumentException("OLX account already added");
        }
        OlxLoginResult login = olxApiClient.login(request.username(), request.password());

        OlxAccount account = new OlxAccount(user.getId(), request.username(), request.password(), request.defaultCityId());
        account.updateToken(login.token(), login.olxUserId(), OffsetDateTime.now().plus(tokenTtl));
        return OlxAccountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional
    public OlxAccountResponse update(User user, Long id, UpdateOlxAccountRequest request) {
        OlxAccount account = findOwned(user, id);

        boolean usernameChanged = request.username() != null && !request.username().equals(account.getUsername());
        boolean passwordChanged = request.password() != null;

        if (usernameChanged && accountRepository.existsByUserIdAndUsername(user.getId(), request.username())) {
            throw new IllegalArgumentException("OLX account already added");
        }

        String effectiveUsername = usernameChanged ? request.username() : account.getUsername();
        if (usernameChanged || passwordChanged) {
            String effectivePassword = passwordChanged ? request.password() : account.getPassword();
            OlxLoginResult login = olxApiClient.login(effectiveUsername, effectivePassword);
            account.updateToken(login.token(), login.olxUserId(), OffsetDateTime.now().plus(tokenTtl));
            account.setUsername(effectiveUsername);
            account.setPassword(effectivePassword);
        }

        if (request.defaultCityId() != null) {
            account.setDefaultCityId(request.defaultCityId());
        }

        return OlxAccountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional
    public void delete(User user, Long id) {
        accountRepository.delete(findOwned(user, id));
    }

    private OlxAccount findOwned(User user, Long id) {
        return accountRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
    }
}
