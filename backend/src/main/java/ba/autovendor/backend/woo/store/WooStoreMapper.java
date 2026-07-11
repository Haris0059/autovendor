package ba.autovendor.backend.woo.store;

import ba.autovendor.backend.woo.store.dto.WooStoreResponse;

public final class WooStoreMapper {
    private WooStoreMapper() {
    }

    public static WooStoreResponse toResponse(WooStore store) {
        return new WooStoreResponse(
                store.getId(),
                store.getName(),
                store.getStoreUrl(),
                store.getCreatedAt()
        );
    }
}
