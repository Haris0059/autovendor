package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxTokenManager;
import ba.autovendor.backend.olx.client.dto.OlxSponsorPriceDto;
import ba.autovendor.backend.olx.sponsor.dto.CreateSponsorRequest;
import ba.autovendor.backend.olx.sponsor.dto.SponsorPriceResponse;
import ba.autovendor.backend.olx.sponsor.dto.SponsorshipResponse;
import ba.autovendor.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SponsorService {

    private static final Logger log = LoggerFactory.getLogger(SponsorService.class);

    static final Set<Integer> SPONSOR_TYPES = Set.of(1, 2);
    static final Set<Integer> SPONSOR_DAYS = Set.of(1, 2, 3, 5, 7, 14, 21, 30);
    static final Set<Integer> REFRESH_EVERY = Set.of(0, 3, 6, 8, 24);
    /** Locations go into the OLX query string unencoded — restrict them to simple tokens. */
    private static final Pattern LOCATION_TOKEN = Pattern.compile("[a-z0-9_-]+");

    private final SponsorshipRepository sponsorshipRepository;
    private final OlxAccountRepository accountRepository;
    private final OlxTokenManager tokenManager;
    private final OlxApiClient olxApiClient;

    public SponsorService(
            SponsorshipRepository sponsorshipRepository,
            OlxAccountRepository accountRepository,
            OlxTokenManager tokenManager,
            OlxApiClient olxApiClient
    ) {
        this.sponsorshipRepository = sponsorshipRepository;
        this.accountRepository = accountRepository;
        this.tokenManager = tokenManager;
        this.olxApiClient = olxApiClient;
    }

    public List<SponsorshipResponse> list(User user) {
        return sponsorshipRepository
                .findAllByUserIdAndEndedAtIsNullAndEndsAtAfterOrderByStartedAtDesc(user.getId(), OffsetDateTime.now())
                .stream()
                .map(SponsorMapper::toResponse)
                .toList();
    }

    public SponsorPriceResponse price(User user, Long accountId, long listingId,
                                      int type, int days, int refreshEvery, List<String> locations) {
        validateSponsorParams(type, days, refreshEvery, locations);
        OlxAccount account = findOwned(user, accountId);
        OlxSponsorPriceDto quote = tokenManager.withAccountToken(account,
                token -> olxApiClient.getSponsorPrice(token, listingId, type, days, refreshEvery, locations));
        return SponsorMapper.toPriceResponse(quote);
    }

    public SponsorshipResponse create(User user, Long accountId, long listingId, CreateSponsorRequest request) {
        List<String> locations = request.locations() == null ? List.of() : request.locations();
        validateSponsorParams(request.type(), request.days(), request.refreshEvery(), locations);
        OlxAccount account = findOwned(user, accountId);

        // Quote first (free) — the undocumented sponsore response carries no usable
        // price, so the tracking row is built from the request + this quote.
        OlxSponsorPriceDto quote = tokenManager.withAccountToken(account, token -> {
            OlxSponsorPriceDto price = olxApiClient.getSponsorPrice(
                    token, listingId, request.type(), request.days(), request.refreshEvery(), locations);
            JsonNode response = olxApiClient.sponsorListing(
                    token, listingId, request.type(), request.days(), request.refreshEvery(), locations);
            log.info("OLX sponsore response for listing {}: {}", listingId, response);
            return price;
        });

        OffsetDateTime now = OffsetDateTime.now();
        endActiveRowsForListing(listingId, now);
        Sponsorship sponsorship = new Sponsorship(
                user.getId(), account.getId(), listingId,
                request.type(), request.days(), request.refreshEvery(),
                SponsorMapper.joinLocations(locations), quote.total(),
                now, now.plusDays(request.days())
        );
        return SponsorMapper.toResponse(sponsorshipRepository.save(sponsorship));
    }

    /**
     * Cancels on OLX via a type-0 re-POST (no delete endpoint exists — unverified
     * live). Upstream 4xx still ends the row: the sponsorship either already
     * expired on OLX or type-0 isn't the cancel — a stuck row helps nobody.
     * Connectivity/5xx propagates and leaves the row active for a retry.
     */
    public void end(User user, Long sponsorshipId) {
        Sponsorship sponsorship = sponsorshipRepository.findByIdAndUserId(sponsorshipId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sponsorship not found"));
        OlxAccount account = accountRepository.findById(sponsorship.getOlxAccountId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
        try {
            tokenManager.withAccountToken(account, token -> {
                olxApiClient.cancelSponsorship(token, sponsorship.getOlxListingId());
                return null;
            });
        } catch (OlxApiException e) {
            if (e.getUpstreamStatus() < 400 || e.getUpstreamStatus() >= 500) {
                throw e;
            }
            log.warn("OLX rejected sponsorship cancel for listing {} ({}); ending the row anyway",
                    sponsorship.getOlxListingId(), e.getMessage());
        }
        sponsorship.end(OffsetDateTime.now());
        sponsorshipRepository.save(sponsorship);
    }

    private void endActiveRowsForListing(Long listingId, OffsetDateTime at) {
        List<Sponsorship> active = sponsorshipRepository.findAllByOlxListingIdAndEndedAtIsNull(listingId);
        for (Sponsorship existing : active) {
            existing.end(at);
        }
        sponsorshipRepository.saveAll(active);
    }

    private void validateSponsorParams(int type, int days, int refreshEvery, List<String> locations) {
        if (!SPONSOR_TYPES.contains(type)) {
            throw new IllegalArgumentException("Sponsor type must be 1 (normal) or 2 (premium)");
        }
        if (!SPONSOR_DAYS.contains(days)) {
            throw new IllegalArgumentException("Sponsor days must be one of " + sorted(SPONSOR_DAYS));
        }
        if (!REFRESH_EVERY.contains(refreshEvery)) {
            throw new IllegalArgumentException("Sponsor refresh_every must be one of " + sorted(REFRESH_EVERY));
        }
        for (String location : locations) {
            if (location == null || !LOCATION_TOKEN.matcher(location).matches()) {
                throw new IllegalArgumentException("Invalid sponsor location: " + location);
            }
        }
    }

    private static List<Integer> sorted(Set<Integer> values) {
        return values.stream().sorted().toList();
    }

    private OlxAccount findOwned(User user, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
    }
}
