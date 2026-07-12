package ba.autovendor.backend.sync;

import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxTokenManager;
import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.woo.client.WooPluginClient;
import ba.autovendor.backend.woo.client.dto.WooPluginProductDto;
import ba.autovendor.backend.woo.store.WooStore;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-product Woo → OLX sync. Deliberately NOT transactional: it spans several
 * network calls, and every DB write (listing id after create, hash after success)
 * must stick even when a later step fails — a retry then resumes as an update.
 * Never throws: every failure is folded into the returned {@link SyncOutcome},
 * which the caller persists as a sync log.
 */
@Component
public class SyncEngine {

    /** OLX's title limit is undocumented; truncate defensively. */
    private static final int TITLE_MAX_LENGTH = 65;
    private static final long COUNTRY_ID_BIH = 49L;

    private final WooPluginClient wooPluginClient;
    private final OlxApiClient olxApiClient;
    private final OlxTokenManager tokenManager;
    private final ImagePipeline imagePipeline;
    private final CategoryMappingRepository mappingRepository;
    private final ProductLinkRepository linkRepository;

    public SyncEngine(WooPluginClient wooPluginClient,
                      OlxApiClient olxApiClient,
                      OlxTokenManager tokenManager,
                      ImagePipeline imagePipeline,
                      CategoryMappingRepository mappingRepository,
                      ProductLinkRepository linkRepository) {
        this.wooPluginClient = wooPluginClient;
        this.olxApiClient = olxApiClient;
        this.tokenManager = tokenManager;
        this.imagePipeline = imagePipeline;
        this.mappingRepository = mappingRepository;
        this.linkRepository = linkRepository;
    }

    public record SyncOutcome(String action, SyncStatus status, String message) {
    }

    /** Manual-trigger entry: fetches the Woo product itself. */
    public SyncOutcome sync(ProductLink link, OlxAccount account, WooStore store) {
        String action = actionFor(link);
        if (link.getSyncDirection() != SyncDirection.woo_to_olx) {
            return new SyncOutcome(action, SyncStatus.skipped, "Sync direction not supported yet");
        }

        WooPluginProductDto product;
        try {
            product = wooPluginClient.getProduct(store.getStoreUrl(), store.getApiKey(), link.getWooProductId());
        } catch (Exception e) {
            return new SyncOutcome(action, SyncStatus.failed, e.getMessage());
        }
        return sync(link, account, product);
    }

    /** Bulk entry (step 8): the product was already fetched from the catalog. */
    public SyncOutcome sync(ProductLink link, OlxAccount account, WooPluginProductDto product) {
        String action = actionFor(link);
        if (link.getSyncDirection() != SyncDirection.woo_to_olx) {
            return new SyncOutcome(action, SyncStatus.skipped, "Sync direction not supported yet");
        }

        try {
            return link.getOlxListingId() != null
                    ? update(link, account, product)
                    : create(link, account, product);
        } catch (Exception e) {
            return new SyncOutcome(action, SyncStatus.failed, e.getMessage());
        }
    }

    private SyncOutcome update(ProductLink link, OlxAccount account, WooPluginProductDto product) {
        // Category mapping is optional on update: when absent, category_id is omitted
        // and the listing keeps its current OLX category.
        Long categoryId = resolveOlxCategoryId(link, product).orElse(null);
        Map<String, Object> payload = buildPayload(product, account, categoryId);

        tokenManager.withAccountToken(account,
                token -> olxApiClient.updateListing(token, link.getOlxListingId(), payload));

        markSynced(link, product);
        return new SyncOutcome("update", SyncStatus.success, "Listing updated");
    }

    private SyncOutcome create(ProductLink link, OlxAccount account, WooPluginProductDto product) {
        if (account.getDefaultCityId() == null) {
            return new SyncOutcome("create", SyncStatus.skipped, "OLX account has no default city set");
        }
        if (product.categories() == null || product.categories().isEmpty()) {
            return new SyncOutcome("create", SyncStatus.skipped, "Product has no WooCommerce category to map");
        }
        var wooCategory = product.categories().getFirst();
        Optional<CategoryMapping> mapping =
                mappingRepository.findByUserIdAndWooCategoryId(link.getUserId(), wooCategory.id());
        if (mapping.isEmpty()) {
            return new SyncOutcome("create", SyncStatus.skipped,
                    "No category mapping for Woo category '" + wooCategory.name() + "' (id " + wooCategory.id() + ")");
        }

        Map<String, Object> payload = buildPayload(product, account, mapping.get().getOlxCategoryId());
        OlxListingDto created = tokenManager.withAccountToken(account,
                token -> olxApiClient.createListing(token, payload));

        // Persist the draft id before images/publish: if a later step fails, the
        // retry takes the update path (and PUT publishes a draft — OLX gotcha).
        link.setOlxListingId(created.id());
        linkRepository.save(link);

        List<String> imageErrors = tokenManager.withAccountToken(account,
                token -> imagePipeline.transfer(token, created.id(), product.images()));

        tokenManager.withAccountToken(account, token -> {
            olxApiClient.publishListing(token, created.id());
            return null;
        });

        markSynced(link, product);
        String message = imageErrors.isEmpty()
                ? "Listing created and published"
                : "Listing created and published; " + String.join("; ", imageErrors);
        return new SyncOutcome("create", SyncStatus.success, message);
    }

    /** Webhook product.deleted: hide the OLX listing but keep the link (reversible). */
    public SyncOutcome hide(ProductLink link, OlxAccount account) {
        if (link.getOlxListingId() == null) {
            return new SyncOutcome("hide", SyncStatus.skipped, "No OLX listing to hide");
        }
        try {
            tokenManager.withAccountToken(account, token -> {
                olxApiClient.hideListing(token, link.getOlxListingId());
                return null;
            });
            return new SyncOutcome("hide", SyncStatus.success, "Listing hidden after WooCommerce product deletion");
        } catch (Exception e) {
            return new SyncOutcome("hide", SyncStatus.failed, e.getMessage());
        }
    }

    private void markSynced(ProductLink link, WooPluginProductDto product) {
        link.markSynced(product.hash(), OffsetDateTime.now());
        linkRepository.save(link);
    }

    private Optional<Long> resolveOlxCategoryId(ProductLink link, WooPluginProductDto product) {
        if (product.categories() == null || product.categories().isEmpty()) {
            return Optional.empty();
        }
        return mappingRepository.findByUserIdAndWooCategoryId(link.getUserId(), product.categories().getFirst().id())
                .map(CategoryMapping::getOlxCategoryId);
    }

    private static String actionFor(ProductLink link) {
        return link.getOlxListingId() != null ? "update" : "create";
    }

    private static Map<String, Object> buildPayload(WooPluginProductDto product, OlxAccount account, Long olxCategoryId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "title", truncate(product.name(), TITLE_MAX_LENGTH));
        put(payload, "short_description", stripHtml(product.shortDescription()));
        put(payload, "description", product.description());
        put(payload, "price", parsePrice(product.price(), product.regularPrice()));
        payload.put("country_id", COUNTRY_ID_BIH);
        put(payload, "city_id", account.getDefaultCityId());
        put(payload, "category_id", olxCategoryId);
        // OLX rejects category-bearing payloads without an attributes field, even when
        // the category has no required attributes. Attribute mapping itself is step 9.
        payload.put("attributes", List.of());
        put(payload, "sku_number", product.sku());
        payload.put("available", "instock".equals(product.stockStatus()));
        payload.put("listing_type", "sell");
        payload.put("state", "new");
        return payload;
    }

    private static void put(Map<String, Object> payload, String key, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            return;
        }
        payload.put(key, value);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private static Double parsePrice(String price, String fallback) {
        Double parsed = tryParse(price);
        return parsed != null ? parsed : tryParse(fallback);
    }

    private static Double tryParse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Woo short descriptions are HTML; OLX renders them as plain text. */
    private static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
