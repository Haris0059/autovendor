package ba.autovendor.backend.sync;

import ba.autovendor.backend.sync.dto.CategoryMappingResponse;
import ba.autovendor.backend.sync.dto.ProductLinkResponse;
import ba.autovendor.backend.sync.dto.SyncLogResponse;

public final class SyncMapper {
    private SyncMapper() {
    }

    public static ProductLinkResponse toResponse(ProductLink link) {
        return new ProductLinkResponse(
                link.getId(),
                link.getOlxAccountId(),
                link.getWooStoreId(),
                link.getOlxListingId(),
                link.getWooProductId(),
                link.getSyncDirection(),
                link.getLastSyncedAt()
        );
    }

    public static CategoryMappingResponse toResponse(CategoryMapping mapping) {
        return new CategoryMappingResponse(
                mapping.getId(),
                mapping.getWooCategoryId(),
                mapping.getWooCategoryName(),
                mapping.getOlxCategoryId(),
                mapping.getOlxCategoryName(),
                mapping.getAttributeDefaults()
        );
    }

    public static SyncLogResponse toResponse(SyncLog log) {
        return new SyncLogResponse(
                log.getId(),
                log.getProductLinkId(),
                log.getAction(),
                log.getStatus(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }
}
