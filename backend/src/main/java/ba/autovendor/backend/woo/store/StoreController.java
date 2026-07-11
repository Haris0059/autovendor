package ba.autovendor.backend.woo.store;

import ba.autovendor.backend.user.User;
import ba.autovendor.backend.woo.store.dto.CreateWooStoreRequest;
import ba.autovendor.backend.woo.store.dto.TestConnectionRequest;
import ba.autovendor.backend.woo.store.dto.TestConnectionResponse;
import ba.autovendor.backend.woo.store.dto.UpdateWooStoreRequest;
import ba.autovendor.backend.woo.store.dto.WooStoreResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/woo/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<WooStoreResponse> list(@AuthenticationPrincipal User user) {
        return storeService.list(user);
    }

    @GetMapping("/{id}")
    public WooStoreResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return storeService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WooStoreResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWooStoreRequest request
    ) {
        return storeService.create(user, request);
    }

    @PutMapping("/{id}")
    public WooStoreResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateWooStoreRequest request
    ) {
        return storeService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        storeService.delete(user, id);
    }

    @PostMapping("/test")
    public TestConnectionResponse test(@Valid @RequestBody TestConnectionRequest request) {
        return storeService.testConnection(request.storeUrl(), request.apiKey());
    }

    @PostMapping("/{id}/test")
    public TestConnectionResponse testStored(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return storeService.testStoredConnection(user, id);
    }
}
