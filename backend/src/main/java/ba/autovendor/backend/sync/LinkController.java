package ba.autovendor.backend.sync;

import ba.autovendor.backend.sync.dto.CreateProductLinkRequest;
import ba.autovendor.backend.sync.dto.ProductLinkResponse;
import ba.autovendor.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sync/links")
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping
    public List<ProductLinkResponse> list(@AuthenticationPrincipal User user) {
        return linkService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductLinkResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateProductLinkRequest request
    ) {
        return linkService.create(user, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        linkService.delete(user, id);
    }
}
