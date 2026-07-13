package ba.autovendor.backend.olx.limit;

import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxTokenManager;
import ba.autovendor.backend.olx.limit.dto.ListingLimitsResponse;
import ba.autovendor.backend.olx.limit.dto.RefreshLimitsResponse;
import ba.autovendor.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LimitService {

    private final OlxAccountRepository accountRepository;
    private final OlxTokenManager tokenManager;
    private final OlxApiClient olxApiClient;

    public LimitService(
            OlxAccountRepository accountRepository,
            OlxTokenManager tokenManager,
            OlxApiClient olxApiClient
    ) {
        this.accountRepository = accountRepository;
        this.tokenManager = tokenManager;
        this.olxApiClient = olxApiClient;
    }

    public ListingLimitsResponse listingLimits(User user, Long accountId) {
        OlxAccount account = findOwned(user, accountId);
        return LimitMapper.toListingLimits(
                tokenManager.withAccountToken(account, olxApiClient::getListingLimits));
    }

    public RefreshLimitsResponse refreshLimits(User user, Long accountId) {
        OlxAccount account = findOwned(user, accountId);
        return LimitMapper.toRefreshLimits(
                tokenManager.withAccountToken(account, olxApiClient::getRefreshLimits));
    }

    private OlxAccount findOwned(User user, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
    }
}
