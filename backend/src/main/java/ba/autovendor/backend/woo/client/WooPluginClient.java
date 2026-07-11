package ba.autovendor.backend.woo.client;

import ba.autovendor.backend.common.InvalidWooApiKeyException;
import ba.autovendor.backend.common.WooPluginException;
import ba.autovendor.backend.woo.client.dto.WooCatalogPageDto;
import ba.autovendor.backend.woo.client.dto.WooHashPageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginAttributeDto;
import ba.autovendor.backend.woo.client.dto.WooPluginCategoryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Client for the AutoVendor WordPress plugin (/wp-json/autovendor/v1) — auth via the
 * X-AutoVendor-API-Key header only; the plugin also accepts an api_key query param,
 * but that would leak keys into WP access logs.
 */
@Component
public class WooPluginClient {

    static final String API_KEY_HEADER = "X-AutoVendor-API-Key";
    private static final String API_BASE_PATH = "/wp-json/autovendor/v1";

    private static final ParameterizedTypeReference<List<WooPluginCategoryDto>> CATEGORIES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<WooPluginAttributeDto>> ATTRIBUTES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WooPluginClient(
            RestClient.Builder builder,
            @Value("${app.woo.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${app.woo.read-timeout-seconds}") long readTimeoutSeconds,
            ObjectMapper objectMapper
    ) {
        // No baseUrl: every user store lives at a different host.
        this.restClient = builder
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()
                        .withTimeouts(Duration.ofSeconds(connectTimeoutSeconds), Duration.ofSeconds(readTimeoutSeconds))))
                .build();
        this.objectMapper = objectMapper;
    }

    /** Trims, defaults to https, strips trailing slashes; the persisted store URL is this form. */
    public static String normalizeStoreUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Store URL is required");
        }
        String url = raw.trim();
        if (!url.matches("(?i)^https?://.*")) {
            url = "https://" + url;
        }
        url = url.replaceAll("/+$", "");
        URI uri = URI.create(url);
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Invalid store URL");
        }
        return url;
    }

    public WooCatalogPageDto getCatalog(String storeUrl, String apiKey, int page, int perPage) {
        return exchange(() -> restClient.get()
                .uri(storeUrl + API_BASE_PATH + "/catalog?page=" + page + "&per_page=" + perPage)
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .body(WooCatalogPageDto.class));
    }

    public WooHashPageDto getCatalogHashes(String storeUrl, String apiKey, int page, int perPage) {
        return exchange(() -> restClient.get()
                .uri(storeUrl + API_BASE_PATH + "/catalog-hashes?page=" + page + "&per_page=" + perPage)
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .body(WooHashPageDto.class));
    }

    public List<WooPluginCategoryDto> getCategories(String storeUrl, String apiKey) {
        List<WooPluginCategoryDto> categories = exchange(() -> restClient.get()
                .uri(storeUrl + API_BASE_PATH + "/categories")
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .body(CATEGORIES));
        return categories != null ? categories : List.of();
    }

    public List<WooPluginAttributeDto> getAttributes(String storeUrl, String apiKey) {
        List<WooPluginAttributeDto> attributes = exchange(() -> restClient.get()
                .uri(storeUrl + API_BASE_PATH + "/attributes")
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .body(ATTRIBUTES));
        return attributes != null ? attributes : List.of();
    }

    private <T> T exchange(Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            throw translate(e);
        } catch (WooPluginException e) {
            throw e;
        } catch (Exception e) {
            throw new WooPluginException("WooCommerce store is unreachable", e);
        }
    }

    private RuntimeException translate(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401 || status == 403) {
            return new InvalidWooApiKeyException("Invalid WooCommerce API key");
        }

        String code = null;
        String detail = "WooCommerce request failed with status " + status;
        try {
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
            JsonNode codeNode = body.path("code");
            if (codeNode.isString()) {
                code = codeNode.asString();
            }
            JsonNode message = body.path("message");
            if (message.isString() && !message.asString().isBlank()) {
                detail = message.asString();
            }
        } catch (Exception ignored) {
            // Non-JSON error body — keep the generic message.
        }

        // WP answers 404 rest_no_route when the plugin isn't installed on the site.
        if (status == 404 && "rest_no_route".equals(code)) {
            return new WooPluginException("AutoVendor plugin not found on this store", e, status);
        }
        return new WooPluginException(detail, e, status);
    }
}
