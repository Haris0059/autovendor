package ba.autovendor.backend.olx.client;

import ba.autovendor.backend.common.InvalidOlxCredentialsException;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.common.OlxAuthException;
import ba.autovendor.backend.olx.client.dto.OlxAttributeDto;
import ba.autovendor.backend.olx.client.dto.OlxCategoryDto;
import ba.autovendor.backend.olx.client.dto.OlxCategorySuggestionDto;
import ba.autovendor.backend.olx.client.dto.OlxCountryDto;
import ba.autovendor.backend.olx.client.dto.OlxImageDto;
import ba.autovendor.backend.olx.client.dto.OlxListingDto;
import ba.autovendor.backend.olx.client.dto.OlxListingPageDto;
import ba.autovendor.backend.olx.client.dto.OlxNamedDto;
import ba.autovendor.backend.olx.client.dto.OlxRefreshLimitsDto;
import ba.autovendor.backend.olx.client.dto.OlxSponsorPriceDto;
import ba.autovendor.backend.olx.client.dto.OlxStateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class OlxApiClient {

    private static final Logger log = LoggerFactory.getLogger(OlxApiClient.class);

    /** Laravel array notation — required by the sponsor price endpoint (see getSponsorPrice). */
    private static final String LOCATIONS_PARAM = "locations[]";

    private static final ParameterizedTypeReference<DataEnvelope<OlxCategoryDto>> CATEGORIES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DataEnvelope<OlxAttributeDto>> ATTRIBUTES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DataEnvelope<OlxNamedDto>> NAMED =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DataEnvelope<OlxCategorySuggestionDto>> SUGGESTIONS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DataEnvelope<OlxCountryDto>> COUNTRIES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DataEnvelope<OlxStateDto>> STATES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String deviceName;
    private final ObjectMapper objectMapper;

    public OlxApiClient(
            RestClient.Builder builder,
            @Value("${app.olx.base-url}") String baseUrl,
            @Value("${app.olx.device-name}") String deviceName,
            ObjectMapper objectMapper
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.deviceName = deviceName;
        this.objectMapper = objectMapper;
    }

    public OlxLoginResult login(String username, String password) {
        OlxLoginResponse response;
        try {
            response = restClient.post()
                    .uri("/auth/login")
                    .body(Map.of(
                            "username", username,
                            "password", password,
                            "device_name", deviceName
                    ))
                    .retrieve()
                    .body(OlxLoginResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new InvalidOlxCredentialsException("Invalid OLX credentials");
            }
            throw new OlxApiException("OLX login failed with status " + e.getStatusCode().value(), e);
        } catch (Exception e) {
            throw new OlxApiException("OLX API is unreachable", e);
        }

        if (response == null || response.token() == null) {
            throw new OlxApiException("OLX login returned an unexpected response");
        }
        return new OlxLoginResult(response.token(), response.user() != null ? response.user().id() : null);
    }

    public List<OlxCategoryDto> getCategories() {
        return get("/categories", CATEGORIES);
    }

    public List<OlxCategoryDto> getCategoryChildren(long categoryId) {
        return get("/categories/" + categoryId, CATEGORIES);
    }

    public List<OlxAttributeDto> getCategoryAttributes(long categoryId) {
        return get("/categories/" + categoryId + "/attributes", ATTRIBUTES);
    }

    public List<OlxCategorySuggestionDto> getCategorySuggestions(String keyword) {
        return get("/categories/suggest?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8),
                SUGGESTIONS);
    }

    public List<OlxNamedDto> getCategoryBrands(long categoryId) {
        return get("/categories/" + categoryId + "/brands", NAMED);
    }

    public List<OlxNamedDto> getBrandModels(long categoryId, long brandId) {
        return get("/categories/" + categoryId + "/brands/" + brandId + "/models", NAMED);
    }

    public List<OlxCountryDto> getCountries() {
        return get("/countries", COUNTRIES);
    }

    public List<OlxStateDto> getCountryStates() {
        return get("/country-states", STATES);
    }

    public List<OlxStateDto> getCitiesTree() {
        return get("/cities", STATES);
    }

    public OlxListingPageDto getActiveListings(String token, String username, int page, Integer perPage) {
        return exchange(() -> restClient.get()
                .uri(listUri("/users/" + username + "/listings", page, perPage))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(OlxListingPageDto.class));
    }

    public OlxListingPageDto getListingsByStatus(String token, long olxUserId, String status, int page, Integer perPage) {
        return exchange(() -> restClient.get()
                .uri(listUri("/users/" + olxUserId + "/listings/" + status, page, perPage))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(OlxListingPageDto.class));
    }

    public OlxListingDto getListing(String token, long listingId) {
        return unwrap(exchange(() -> restClient.get()
                .uri("/listings/" + listingId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JsonNode.class)), OlxListingDto.class);
    }

    public OlxListingDto createListing(String token, Map<String, Object> payload) {
        return unwrap(exchange(() -> restClient.post()
                .uri("/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(payload)
                .retrieve()
                .body(JsonNode.class)), OlxListingDto.class);
    }

    public OlxListingDto updateListing(String token, long listingId, Map<String, Object> payload) {
        return unwrap(exchange(() -> restClient.put()
                .uri("/listings/" + listingId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(payload)
                .retrieve()
                .body(JsonNode.class)), OlxListingDto.class);
    }

    public void deleteListing(String token, long listingId) {
        exchange(() -> restClient.delete()
                .uri("/listings/" + listingId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity());
    }

    public void publishListing(String token, long listingId) {
        actionPost(token, listingId, "publish");
    }

    public void finishListing(String token, long listingId) {
        actionPost(token, listingId, "finish");
    }

    public void hideListing(String token, long listingId) {
        actionPost(token, listingId, "hide");
    }

    public void unhideListing(String token, long listingId) {
        actionPost(token, listingId, "unhide");
    }

    public void refreshListing(String token, long listingId) {
        exchange(() -> restClient.put()
                .uri("/listings/" + listingId + "/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity());
    }

    public List<OlxImageDto> uploadImages(String token, long listingId, List<MultipartFile> images) {
        return uploadImageResources(token, listingId,
                images.stream().map(MultipartFile::getResource).map(r -> (Resource) r).toList());
    }

    public List<OlxImageDto> uploadImageResources(String token, long listingId, List<Resource> images) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        for (Resource image : images) {
            body.part("images[]", image);
        }
        JsonNode node = exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/image-upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(JsonNode.class));
        return parseImages(node);
    }

    /** Server-side upload of a remote image; OLX fetches the URL itself (documented `image_url` param). */
    public List<OlxImageDto> uploadImageByUrl(String token, long listingId, String imageUrl) {
        JsonNode node = exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/image-upload")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of("image_url", imageUrl))
                .retrieve()
                .body(JsonNode.class));
        return parseImages(node);
    }

    private List<OlxImageDto> parseImages(JsonNode node) {
        JsonNode data = node != null && node.has("data") ? node.get("data") : node;
        return objectMapper.convertValue(data,
                objectMapper.getTypeFactory().constructCollectionType(List.class, OlxImageDto.class));
    }

    public void deleteImage(String token, long listingId, long imageId) {
        exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/image-delete")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of("imageId", imageId))
                .retrieve()
                .toBodilessEntity());
    }

    public void setMainImage(String token, long listingId, long imageId) {
        exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/image-main")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of("imageId", imageId))
                .retrieve()
                .toBodilessEntity());
    }

    /**
     * Quote for sponsoring a listing (free). OLX requires the Laravel array
     * notation {@code locations[]=} — a plain {@code locations=} 422s with
     * "Zona treba biti niz" (pinned live July 2026). Values are validated
     * upstream to simple tokens, so they are appended unencoded.
     */
    public OlxSponsorPriceDto getSponsorPrice(String token, long listingId, int type, int days,
                                              int refreshEvery, List<String> locations) {
        StringBuilder uri = new StringBuilder("/listings/" + listingId + "/sponsore/price"
                + "?type=" + type + "&days=" + days + "&refresh_every=" + refreshEvery);
        for (String location : locations) {
            uri.append('&').append(LOCATIONS_PARAM).append('=').append(location);
        }
        return unwrap(exchange(() -> restClient.get()
                .uri(uri.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(JsonNode.class)), OlxSponsorPriceDto.class);
    }

    /** POST /listings/{id}/sponsore (OLX spelling). Charges OLX credits. Response shape is undocumented. */
    public JsonNode sponsorListing(String token, long listingId, int type, int days,
                                   int refreshEvery, List<String> locations) {
        return exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/sponsore")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of(
                        "type", type,
                        "days", days,
                        "refresh_every", refreshEvery,
                        "locations", locations
                ))
                .retrieve()
                .body(JsonNode.class));
    }

    /**
     * OLX documents no delete-sponsor endpoint; type 0 is documented as "no
     * sponsoring", so a re-POST with it is our cancel. UNVERIFIED live (a real
     * cancel test would first require paying for a sponsorship).
     */
    public void cancelSponsorship(String token, long listingId) {
        exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/sponsore")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of("type", 0))
                .retrieve()
                .toBodilessEntity());
    }

    /** OLX takes only the new (discounted) price; the original stays on the listing itself. */
    public void createDiscount(String token, long listingId, BigDecimal price, int days) {
        exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/discount")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of("price", price, "days", days))
                .retrieve()
                .toBodilessEntity());
    }

    public void finishDiscount(String token, long listingId) {
        exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/discount/finish")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity());
    }

    /**
     * Raw node because the live shape differs from the docs (pinned July 2026):
     * {"data": {"cars"|"real-estate"|"car-parts"|"other": {limit, unlimited, listings}}}.
     * The mapper in olx/limit tolerates drift instead of failing here.
     */
    public JsonNode getListingLimits(String token) {
        return authGet("/listing-limits", token, JsonNode.class);
    }

    public OlxRefreshLimitsDto getRefreshLimits(String token) {
        return authGet("/listing/refresh/limits", token, OlxRefreshLimitsDto.class);
    }

    private void actionPost(String token, long listingId, String action) {
        exchange(() -> restClient.post()
                .uri("/listings/" + listingId + "/" + action)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity());
    }

    private static String listUri(String path, int page, Integer perPage) {
        String uri = path + "?page=" + page;
        return perPage != null ? uri + "&per_page=" + perPage : uri;
    }

    private <T> T unwrap(JsonNode node, Class<T> type) {
        if (node == null) {
            throw new OlxApiException("OLX returned an empty response");
        }
        JsonNode data = node.has("data") ? node.get("data") : node;
        return objectMapper.convertValue(data, type);
    }

    /**
     * Runs an authenticated OLX call, translating auth rejections (401/403) into
     * {@link OlxAuthException} so {@link OlxTokenManager#withAccountToken} can re-login once.
     */
    private <T> T exchange(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new OlxAuthException("OLX rejected the account token");
            }
            throw olxError(e);
        } catch (OlxApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OlxApiException("OLX API is unreachable", e);
        }
    }

    /** Surfaces OLX's own error message (e.g. refresh limits) instead of a bare status code. */
    private OlxApiException olxError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String detail = "OLX request failed with status " + status;
        log.warn("OLX error response ({}): {}", status, e.getResponseBodyAsString());
        try {
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
            // Validation failures arrive nested: {"error": {"message": ..., "errors": {...}}}
            if (body.get("error") != null && body.get("error").isObject()) {
                body = body.get("error");
            }
            JsonNode message = body.has("message") ? body.get("message") : body.get("error");
            if (message != null && message.isString() && !message.asString().isBlank()) {
                detail = message.asString();
            }
            JsonNode errors = body.get("errors");
            if (errors != null && !errors.isNull() && !errors.isEmpty()) {
                detail = detail + ": " + errors;
            }
        } catch (Exception ignored) {
            // Non-JSON error body — keep the generic message.
        }
        return new OlxApiException(detail, e, status);
    }

    <T> T authGet(String path, String token, Class<T> type) {
        return exchange(() -> restClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(type));
    }

    private <T> List<T> get(String path, ParameterizedTypeReference<DataEnvelope<T>> type) {
        DataEnvelope<T> envelope;
        try {
            envelope = restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException e) {
            throw olxError(e);
        } catch (Exception e) {
            throw new OlxApiException("OLX API is unreachable", e);
        }
        return envelope != null && envelope.data() != null ? envelope.data() : List.of();
    }

    record DataEnvelope<T>(List<T> data) {
    }

    record OlxLoginResponse(String token, OlxUser user) {
        record OlxUser(Long id) {
        }
    }
}
