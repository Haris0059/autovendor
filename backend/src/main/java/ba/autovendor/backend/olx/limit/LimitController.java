package ba.autovendor.backend.olx.limit;

import ba.autovendor.backend.olx.limit.dto.ListingLimitsResponse;
import ba.autovendor.backend.olx.limit.dto.RefreshLimitsResponse;
import ba.autovendor.backend.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/olx/accounts/{accountId}")
public class LimitController {

    private final LimitService limitService;

    public LimitController(LimitService limitService) {
        this.limitService = limitService;
    }

    @GetMapping("/listing-limits")
    public ListingLimitsResponse listingLimits(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId
    ) {
        return limitService.listingLimits(user, accountId);
    }

    @GetMapping("/listing/refresh/limits")
    public RefreshLimitsResponse refreshLimits(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId
    ) {
        return limitService.refreshLimits(user, accountId);
    }
}
