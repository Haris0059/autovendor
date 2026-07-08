package ba.autovendor.backend.olx.listing;

import ba.autovendor.backend.olx.listing.dto.ListingResponse;
import ba.autovendor.backend.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AllListingsController {

    private final ListingService listingService;

    public AllListingsController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/olx/listings/all")
    public List<ListingResponse> all(@AuthenticationPrincipal User user) {
        return listingService.getAll(user);
    }
}
