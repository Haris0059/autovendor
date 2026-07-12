package ba.autovendor.backend.sync.dto;

import ba.autovendor.backend.sync.SyncDirection;
import jakarta.validation.constraints.NotNull;

public record CreateProductLinkRequest(
        @NotNull Long olxAccountId,
        @NotNull Long wooStoreId,
        Long olxListingId,
        @NotNull Long wooProductId,
        @NotNull SyncDirection syncDirection
) {
}
