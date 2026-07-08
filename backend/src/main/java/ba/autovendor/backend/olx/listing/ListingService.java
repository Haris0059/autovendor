package ba.autovendor.backend.olx.listing;

import ba.autovendor.backend.common.PageResponse;
import ba.autovendor.backend.olx.account.OlxAccount;
import ba.autovendor.backend.olx.account.OlxAccountRepository;
import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.OlxTokenManager;
import ba.autovendor.backend.olx.client.dto.OlxImageDto;
import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.olx.client.dto.OlxListingPageDto;
import ba.autovendor.backend.olx.listing.dto.CreateListingRequest;
import ba.autovendor.backend.olx.listing.dto.ListingResponse;
import ba.autovendor.backend.olx.listing.dto.UpdateListingRequest;
import ba.autovendor.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
// Cached values must be ArrayLists: the Redis JSON serializer cannot reconstruct
// JDK immutable lists (List.of / Stream.toList) from their embedded type hints.
public class ListingService {

    private static final List<String> ALL_STATUSES =
            List.of("active", "finished", "inactive", "expired", "hidden");
    private static final int MAX_PAGES_PER_STATUS = 10;

    private final OlxAccountRepository accountRepository;
    private final OlxApiClient olxApiClient;
    private final OlxTokenManager tokenManager;

    public ListingService(
            OlxAccountRepository accountRepository,
            OlxApiClient olxApiClient,
            OlxTokenManager tokenManager
    ) {
        this.accountRepository = accountRepository;
        this.olxApiClient = olxApiClient;
        this.tokenManager = tokenManager;
    }

    public PageResponse<ListingResponse> list(User user, Long accountId, String status, int page, Integer perPage) {
        OlxAccount account = findOwned(user, accountId);
        OlxListingPageDto result = tokenManager.withAccountToken(account,
                token -> fetchPage(token, account, status, page, perPage));

        List<ListingResponse> data = new ArrayList<>();
        if (result.data() != null) {
            String override = statusOverride(status);
            for (OlxListingDto dto : result.data()) {
                data.add(ListingMapper.toResponse(dto, account.getId(), override));
            }
        }
        OlxListingPageDto.OlxPageMetaDto meta = result.meta();
        return new PageResponse<>(
                data,
                meta != null && meta.total() != null ? meta.total() : data.size(),
                meta != null && meta.currentPage() != null ? meta.currentPage() : page,
                meta != null && meta.perPage() != null ? meta.perPage() : data.size(),
                meta != null && meta.lastPage() != null ? meta.lastPage() : 1
        );
    }

    @Cacheable(cacheNames = "olx-listings-all", key = "#user.id")
    public List<ListingResponse> getAll(User user) {
        List<ListingResponse> all = new ArrayList<>();
        for (OlxAccount account : accountRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())) {
            tokenManager.withAccountToken(account, token -> {
                for (String status : ALL_STATUSES) {
                    int page = 1;
                    while (page <= MAX_PAGES_PER_STATUS) {
                        OlxListingPageDto result = fetchPage(token, account, status, page, null);
                        if (result.data() == null || result.data().isEmpty()) {
                            break;
                        }
                        for (OlxListingDto dto : result.data()) {
                            all.add(ListingMapper.toResponse(dto, account.getId(), statusOverride(status)));
                        }
                        OlxListingPageDto.OlxPageMetaDto meta = result.meta();
                        if (meta == null || meta.lastPage() == null || page >= meta.lastPage()) {
                            break;
                        }
                        page++;
                    }
                }
                return null;
            });
        }
        return all;
    }

    public ListingResponse get(User user, Long accountId, long listingId) {
        OlxAccount account = findOwned(user, accountId);
        OlxListingDto dto = tokenManager.withAccountToken(account,
                token -> olxApiClient.getListing(token, listingId));
        return ListingMapper.toResponse(dto, account.getId());
    }

    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public ListingResponse create(User user, Long accountId, CreateListingRequest request) {
        OlxAccount account = findOwned(user, accountId);
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "title", request.title());
        put(payload, "short_description", request.shortDescription());
        put(payload, "description", request.description());
        put(payload, "price", request.price());
        put(payload, "country_id", request.countryId());
        put(payload, "city_id", request.cityId());
        put(payload, "category_id", request.categoryId());
        put(payload, "brand_id", request.brandId());
        put(payload, "model_id", request.modelId());
        put(payload, "sku_number", request.skuNumber());
        put(payload, "available", request.available());
        put(payload, "listing_type", request.listingType());
        put(payload, "state", request.state());
        put(payload, "attributes", request.attributes());

        OlxListingDto dto = tokenManager.withAccountToken(account,
                token -> olxApiClient.createListing(token, payload));
        return ListingMapper.toResponse(dto, account.getId());
    }

    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public ListingResponse update(User user, Long accountId, long listingId, UpdateListingRequest request) {
        OlxAccount account = findOwned(user, accountId);
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "title", request.title());
        put(payload, "short_description", request.shortDescription());
        put(payload, "description", request.description());
        put(payload, "price", request.price());
        put(payload, "country_id", request.countryId());
        put(payload, "city_id", request.cityId());
        put(payload, "category_id", request.categoryId());
        put(payload, "brand_id", request.brandId());
        put(payload, "model_id", request.modelId());
        put(payload, "sku_number", request.skuNumber());
        put(payload, "available", request.available());
        put(payload, "listing_type", request.listingType());
        put(payload, "state", request.state());
        put(payload, "attributes", request.attributes());

        OlxListingDto dto = tokenManager.withAccountToken(account,
                token -> olxApiClient.updateListing(token, listingId, payload));
        return ListingMapper.toResponse(dto, account.getId());
    }

    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public void delete(User user, Long accountId, long listingId) {
        OlxAccount account = findOwned(user, accountId);
        tokenManager.withAccountToken(account, token -> {
            olxApiClient.deleteListing(token, listingId);
            return null;
        });
    }

    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public void action(User user, Long accountId, long listingId, ListingAction action) {
        OlxAccount account = findOwned(user, accountId);
        tokenManager.withAccountToken(account, token -> {
            switch (action) {
                case PUBLISH -> olxApiClient.publishListing(token, listingId);
                case FINISH -> olxApiClient.finishListing(token, listingId);
                case HIDE -> olxApiClient.hideListing(token, listingId);
                case UNHIDE -> olxApiClient.unhideListing(token, listingId);
                case REFRESH -> olxApiClient.refreshListing(token, listingId);
            }
            return null;
        });
    }

    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public List<ListingResponse.ListingImageResponse> uploadImages(
            User user, Long accountId, long listingId, List<MultipartFile> images) {
        OlxAccount account = findOwned(user, accountId);
        List<OlxImageDto> uploaded = tokenManager.withAccountToken(account,
                token -> olxApiClient.uploadImages(token, listingId, images));

        List<ListingResponse.ListingImageResponse> result = new ArrayList<>();
        for (OlxImageDto image : uploaded) {
            String url = image.sizes() != null
                    ? image.sizes().getOrDefault("lg", image.sizes().get("sm"))
                    : null;
            result.add(new ListingResponse.ListingImageResponse(
                    image.id(), url, Boolean.TRUE.equals(image.main())));
        }
        return result;
    }

    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public void deleteImage(User user, Long accountId, long listingId, long imageId) {
        OlxAccount account = findOwned(user, accountId);
        tokenManager.withAccountToken(account, token -> {
            olxApiClient.deleteImage(token, listingId, imageId);
            return null;
        });
    }

    @CacheEvict(cacheNames = "olx-listings-all", key = "#user.id")
    public void setMainImage(User user, Long accountId, long listingId, long imageId) {
        OlxAccount account = findOwned(user, accountId);
        tokenManager.withAccountToken(account, token -> {
            olxApiClient.setMainImage(token, listingId, imageId);
            return null;
        });
    }

    public enum ListingAction {
        PUBLISH, FINISH, HIDE, UNHIDE, REFRESH
    }

    private OlxListingPageDto fetchPage(String token, OlxAccount account, String status, int page, Integer perPage) {
        String effective = status == null || status.isBlank() ? "active" : status;
        if (effective.equals("active")) {
            return olxApiClient.getActiveListings(token, account.getUsername(), page, perPage);
        }
        // OLX has no dedicated draft endpoint — drafts live under /inactive.
        String olxStatus = effective.equals("draft") ? "inactive" : effective;
        if (account.getOlxUserId() == null) {
            throw new EntityNotFoundException("OLX account is missing its OLX user id");
        }
        return olxApiClient.getListingsByStatus(token, account.getOlxUserId(), olxStatus, page, perPage);
    }

    private static String statusOverride(String requestedStatus) {
        return "hidden".equals(requestedStatus) ? "hidden" : null;
    }

    private void put(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private OlxAccount findOwned(User user, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("OLX account not found"));
    }
}
