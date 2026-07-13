package ba.autovendor.backend.olx.sponsor;

import ba.autovendor.backend.olx.sponsor.dto.CreateDiscountRequest;
import ba.autovendor.backend.olx.sponsor.dto.DiscountResponse;
import ba.autovendor.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Same scope mix as SponsorController — see the note there. */
@RestController
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping("/olx/discounts")
    public List<DiscountResponse> list(@AuthenticationPrincipal User user) {
        return discountService.list(user);
    }

    @PostMapping("/olx/accounts/{accountId}/listings/{listingId}/discount")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse create(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId,
            @Valid @RequestBody CreateDiscountRequest request
    ) {
        return discountService.create(user, accountId, listingId, request);
    }

    @PostMapping("/olx/discounts/{id}/finish")
    public void finish(@AuthenticationPrincipal User user, @PathVariable Long id) {
        discountService.finish(user, id);
    }
}
