package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.olx.sponsor.dto.CreateSponsorRequest;
import ba.autovendor.backend.olx.sponsor.dto.SponsorPriceResponse;
import ba.autovendor.backend.olx.sponsor.dto.SponsorshipResponse;
import ba.autovendor.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Routes deliberately mix scopes: the list/end operations are user-wide /
 * row-scoped (the Sponzorstva page has no account context; tracking rows know
 * their account), while quote/create need the account path for the OLX token.
 */
@RestController
public class SponsorController {

    private final SponsorService sponsorService;

    public SponsorController(SponsorService sponsorService) {
        this.sponsorService = sponsorService;
    }

    @GetMapping("/olx/sponsored")
    public List<SponsorshipResponse> list(@AuthenticationPrincipal User user) {
        return sponsorService.list(user);
    }

    @GetMapping("/olx/accounts/{accountId}/listings/{listingId}/sponsored/price")
    public SponsorPriceResponse price(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId,
            @RequestParam int type,
            @RequestParam int days,
            @RequestParam(name = "refresh_every") int refreshEvery,
            @RequestParam(required = false) String locations
    ) {
        return sponsorService.price(user, accountId, listingId, type, days, refreshEvery,
                SponsorMapper.splitLocations(locations));
    }

    @PostMapping("/olx/accounts/{accountId}/listings/{listingId}/sponsored")
    @ResponseStatus(HttpStatus.CREATED)
    public SponsorshipResponse create(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId,
            @Valid @RequestBody CreateSponsorRequest request
    ) {
        return sponsorService.create(user, accountId, listingId, request);
    }

    @DeleteMapping("/olx/sponsored/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(@AuthenticationPrincipal User user, @PathVariable Long id) {
        sponsorService.end(user, id);
    }
}
