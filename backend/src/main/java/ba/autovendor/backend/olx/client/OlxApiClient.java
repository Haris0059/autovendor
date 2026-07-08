package ba.autovendor.backend.olx.client;

import ba.autovendor.backend.common.InvalidOlxCredentialsException;
import ba.autovendor.backend.common.OlxApiException;
import ba.autovendor.backend.common.OlxAuthException;
import ba.autovendor.backend.olx.client.dto.OlxAttributeDto;
import ba.autovendor.backend.olx.client.dto.OlxCategoryDto;
import ba.autovendor.backend.olx.client.dto.OlxCountryDto;
import ba.autovendor.backend.olx.client.dto.OlxNamedDto;
import ba.autovendor.backend.olx.client.dto.OlxStateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class OlxApiClient {

    private static final ParameterizedTypeReference<DataEnvelope<OlxCategoryDto>> CATEGORIES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DataEnvelope<OlxAttributeDto>> ATTRIBUTES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DataEnvelope<OlxNamedDto>> NAMED =
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

    public OlxApiClient(
            RestClient.Builder builder,
            @Value("${app.olx.base-url}") String baseUrl,
            @Value("${app.olx.device-name}") String deviceName
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.deviceName = deviceName;
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

    private <T> List<T> get(String path, ParameterizedTypeReference<DataEnvelope<T>> type) {
        DataEnvelope<T> envelope;
        try {
            envelope = restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException e) {
            throw new OlxApiException("OLX request " + path + " failed with status " + e.getStatusCode().value(), e);
        } catch (Exception e) {
            throw new OlxApiException("OLX API is unreachable", e);
        }
        return envelope != null && envelope.data() != null ? envelope.data() : List.of();
    }

    <T> T authGet(String path, String token, Class<T> type) {
        try {
            return restClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new OlxAuthException("OLX rejected the account token");
            }
            throw new OlxApiException("OLX request " + path + " failed with status " + status, e);
        } catch (Exception e) {
            throw new OlxApiException("OLX API is unreachable", e);
        }
    }

    record DataEnvelope<T>(List<T> data) {
    }

    record OlxLoginResponse(String token, OlxUser user) {
        record OlxUser(Long id) {
        }
    }
}
