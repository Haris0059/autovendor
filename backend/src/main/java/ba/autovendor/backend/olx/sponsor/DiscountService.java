package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxTokenManager;
import ba.autovendor.backend.olx.sponsor.dto.CreateDiscountRequest;
import ba.autovendor.backend.olx.sponsor.dto.DiscountResponse;
import ba.autovendor.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class DiscountService {

    static final Set<Integer> DISCOUNT_DAYS = Set.of(3, 7, 30);

    private final DiscountRepository discountRepository;
    private final OlxAccountRepository accountRepository;
    private final OlxTokenManager tokenManager;
    private final OlxApiClient olxApiClient;

    public DiscountService(
            DiscountRepository discountRepository,
            OlxAccountRepository accountRepository,
            OlxTokenManager tokenManager,
            OlxApiClient olxApiClient
    ) {
        this.discountRepository = discountRepository;
        this.accountRepository = accountRepository;
        this.tokenManager = tokenManager;
        this.olxApiClient = olxApiClient;
    }

    public List<DiscountResponse> list(User user) {
        return discountRepository
                .findAllByUserIdAndEndedAtIsNullAndEndsAtAfterOrderByStartedAtDesc(user.getId(), OffsetDateTime.now())
                .stream()
                .map(DiscountMapper::toResponse)
                .toList();
    }

    public DiscountResponse create(User user, Long accountId, long listingId, CreateDiscountRequest request) {
        validate(request);
        OlxAccount account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));

        // OLX only takes the new price; original_price lives on our tracking row.
        tokenManager.withAccountToken(account, token -> {
            olxApiClient.createDiscount(token, listingId, request.discountPrice(), request.days());
            return null;
        });

        OffsetDateTime now = OffsetDateTime.now();
        endActiveRowsForListing(listingId, now);
        Discount discount = new Discount(
                user.getId(), account.getId(), listingId,
                request.originalPrice(), request.discountPrice(), request.days(),
                now, now.plusDays(request.days())
        );
        return DiscountMapper.toResponse(discountRepository.save(discount));
    }

    /**
     * Unlike the sponsor cancel, discount/finish is a documented endpoint that is
     * live-verified — upstream errors are meaningful, so they propagate and the
     * row stays active.
     */
    public void finish(User user, Long discountId) {
        Discount discount = discountRepository.findByIdAndUserId(discountId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Discount not found"));
        OlxAccount account = accountRepository.findById(discount.getOlxAccountId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
        tokenManager.withAccountToken(account, token -> {
            olxApiClient.finishDiscount(token, discount.getOlxListingId());
            return null;
        });
        discount.end(OffsetDateTime.now());
        discountRepository.save(discount);
    }

    private void endActiveRowsForListing(Long listingId, OffsetDateTime at) {
        List<Discount> active = discountRepository.findAllByOlxListingIdAndEndedAtIsNull(listingId);
        for (Discount existing : active) {
            existing.end(at);
        }
        discountRepository.saveAll(active);
    }

    private void validate(CreateDiscountRequest request) {
        if (!DISCOUNT_DAYS.contains(request.days())) {
            throw new IllegalArgumentException("Discount days must be one of [3, 7, 30]");
        }
        if (request.originalPrice().compareTo(BigDecimal.ZERO) <= 0
                || request.discountPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Prices must be greater than zero");
        }
        if (request.discountPrice().compareTo(request.originalPrice()) >= 0) {
            throw new IllegalArgumentException("Discount price must be below the original price");
        }
    }
}
