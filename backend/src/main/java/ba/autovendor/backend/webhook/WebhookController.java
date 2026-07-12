package ba.autovendor.backend.webhook;

import ba.autovendor.backend.woo.store.WooStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public endpoint (permitAll) for the WP plugin's product events; the caller is
 * authenticated by the store API key it presents, not by a JWT.
 */
@RestController
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "X-AutoVendor-Key", required = false) String apiKey,
            @Valid @RequestBody WebhookEventRequest event) {
        List<WooStore> stores = webhookService.authenticate(event.siteUrl(), apiKey);
        if (stores.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("detail", "Unknown store or invalid key"));
        }
        int processed = webhookService.process(stores, event);
        return ResponseEntity.ok(Map.of("processed", processed));
    }
}
