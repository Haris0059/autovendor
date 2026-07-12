package ba.autovendor.backend.sync.dto;

import jakarta.validation.constraints.NotNull;

public record TriggerSyncRequest(
        @NotNull Long productLinkId
) {
}
