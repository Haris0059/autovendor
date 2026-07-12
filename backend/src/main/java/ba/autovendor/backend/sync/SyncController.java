package ba.autovendor.backend.sync;

import ba.autovendor.backend.common.PageResponse;
import ba.autovendor.backend.sync.dto.SyncLogResponse;
import ba.autovendor.backend.sync.dto.TriggerSyncRequest;
import ba.autovendor.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/sync")
    public SyncLogResponse trigger(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TriggerSyncRequest request
    ) {
        return syncService.trigger(user, request.productLinkId());
    }

    @GetMapping("/sync/history")
    public PageResponse<SyncLogResponse> history(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) SyncStatus status,
            @RequestParam(name = "account_id", required = false) Long accountId,
            @RequestParam(name = "store_id", required = false) Long storeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", required = false) Integer perPage
    ) {
        return syncService.history(user, status, accountId, storeId, page, perPage);
    }
}
