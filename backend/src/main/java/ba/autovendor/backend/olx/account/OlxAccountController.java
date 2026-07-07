package ba.autovendor.backend.olx.account;

import ba.autovendor.backend.olx.account.dto.CreateOlxAccountRequest;
import ba.autovendor.backend.olx.account.dto.OlxAccountResponse;
import ba.autovendor.backend.olx.account.dto.UpdateOlxAccountRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/olx/accounts")
public class OlxAccountController {

    private final OlxAccountService accountService;

    public OlxAccountController(OlxAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<OlxAccountResponse> list(@AuthenticationPrincipal User user) {
        return accountService.list(user);
    }

    @GetMapping("/{id}")
    public OlxAccountResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return accountService.get(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OlxAccountResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateOlxAccountRequest request
    ) {
        return accountService.create(user, request);
    }

    @PutMapping("/{id}")
    public OlxAccountResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOlxAccountRequest request
    ) {
        return accountService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        accountService.delete(user, id);
    }
}
