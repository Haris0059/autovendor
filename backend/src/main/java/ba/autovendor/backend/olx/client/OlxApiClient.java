package ba.autovendor.backend.olx.client;

import ba.autovendor.backend.common.InvalidOlxCredentialsException;
import ba.autovendor.backend.common.OlxApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class OlxApiClient {

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

    record OlxLoginResponse(String token, OlxUser user) {
        record OlxUser(Long id) {
        }
    }
}
