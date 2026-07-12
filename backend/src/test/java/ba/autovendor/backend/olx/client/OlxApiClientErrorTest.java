package ba.autovendor.backend.olx.client;

import ba.autovendor.backend.common.OlxApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Pins how OLX error bodies are translated into {@link OlxApiException} messages.
 * OLX wraps validation failures in a nested envelope:
 * {@code {"error": {"message": ..., "errors": {...}}}} (observed live on create-listing 422s).
 */
class OlxApiClientErrorTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final OlxApiClient client =
            new OlxApiClient(builder, "https://api.olx.test", "AutoVendor", new ObjectMapper());

    @Test
    void nestedValidationErrorSurfacesMessageAndFieldErrors() {
        server.expect(requestTo("https://api.olx.test/listings"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"error":{"type":"validation_failed","status":422,
                                "message":"Podaci koje ste poslali nisu potpuni ili su u neispravnom formatu.",
                                "errors":{"category_id":"kategorija zahtjeva prisutno polje attributes"}}}
                                """));

        assertThatThrownBy(() -> client.createListing("token", Map.of()))
                .isInstanceOf(OlxApiException.class)
                .hasMessageContaining("Podaci koje ste poslali nisu potpuni")
                .hasMessageContaining("kategorija zahtjeva prisutno polje attributes");
    }

    @Test
    void flatErrorMessageIsSurfaced() {
        server.expect(requestTo("https://api.olx.test/listings"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Previše zahtjeva\"}"));

        assertThatThrownBy(() -> client.createListing("token", Map.of()))
                .isInstanceOf(OlxApiException.class)
                .hasMessageContaining("Previše zahtjeva");
    }

    @Test
    void nonJsonBodyFallsBackToGenericStatusMessage() {
        server.expect(requestTo("https://api.olx.test/listings"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html>Bad Gateway</html>"));

        assertThatThrownBy(() -> client.createListing("token", Map.of()))
                .isInstanceOf(OlxApiException.class)
                .hasMessageContaining("OLX request failed with status 502");
    }
}
