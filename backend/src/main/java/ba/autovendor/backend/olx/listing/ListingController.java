package ba.autovendor.backend.olx.listing;

import ba.autovendor.backend.common.PageResponse;
import ba.autovendor.backend.olx.listing.dto.CreateListingRequest;
import ba.autovendor.backend.olx.listing.dto.ListingResponse;
import ba.autovendor.backend.olx.listing.dto.UpdateListingRequest;
import ba.autovendor.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/olx/accounts/{accountId}/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public PageResponse<ListingResponse> list(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", required = false) Integer perPage
    ) {
        return listingService.list(user, accountId, status, page, perPage);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @Valid @RequestBody CreateListingRequest request
    ) {
        return listingService.create(user, accountId, request);
    }

    @GetMapping("/{listingId}")
    public ListingResponse get(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId
    ) {
        return listingService.get(user, accountId, listingId);
    }

    @PutMapping("/{listingId}")
    public ListingResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        return listingService.update(user, accountId, listingId, request);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId
    ) {
        listingService.delete(user, accountId, listingId);
    }

    @PostMapping("/{listingId}/publish")
    public void publish(@AuthenticationPrincipal User user, @PathVariable Long accountId, @PathVariable long listingId) {
        listingService.action(user, accountId, listingId, ListingService.ListingAction.PUBLISH);
    }

    @PostMapping("/{listingId}/finish")
    public void finish(@AuthenticationPrincipal User user, @PathVariable Long accountId, @PathVariable long listingId) {
        listingService.action(user, accountId, listingId, ListingService.ListingAction.FINISH);
    }

    @PostMapping("/{listingId}/hide")
    public void hide(@AuthenticationPrincipal User user, @PathVariable Long accountId, @PathVariable long listingId) {
        listingService.action(user, accountId, listingId, ListingService.ListingAction.HIDE);
    }

    @PostMapping("/{listingId}/unhide")
    public void unhide(@AuthenticationPrincipal User user, @PathVariable Long accountId, @PathVariable long listingId) {
        listingService.action(user, accountId, listingId, ListingService.ListingAction.UNHIDE);
    }

    @PutMapping("/{listingId}/refresh")
    public void refresh(@AuthenticationPrincipal User user, @PathVariable Long accountId, @PathVariable long listingId) {
        listingService.action(user, accountId, listingId, ListingService.ListingAction.REFRESH);
    }

    @PostMapping("/{listingId}/images")
    public List<ListingResponse.ListingImageResponse> uploadImages(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId,
            @RequestParam("images") List<MultipartFile> images
    ) {
        return listingService.uploadImages(user, accountId, listingId, images);
    }

    @DeleteMapping("/{listingId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId,
            @PathVariable long imageId
    ) {
        listingService.deleteImage(user, accountId, listingId, imageId);
    }

    @PostMapping("/{listingId}/images/{imageId}/main")
    public void setMainImage(
            @AuthenticationPrincipal User user,
            @PathVariable Long accountId,
            @PathVariable long listingId,
            @PathVariable long imageId
    ) {
        listingService.setMainImage(user, accountId, listingId, imageId);
    }
}
