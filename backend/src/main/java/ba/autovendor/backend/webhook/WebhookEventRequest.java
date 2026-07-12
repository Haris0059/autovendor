package ba.autovendor.backend.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload the WP plugin POSTs on product changes (see wp-plguin autovendor.php send_webhook). */
public record WebhookEventRequest(
        @NotBlank String event,
        @NotNull Long productId,
        @NotBlank String siteUrl,
        String timestamp
) {
}
