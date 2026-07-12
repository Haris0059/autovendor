package ba.autovendor.backend.sync;

import ba.autovendor.backend.olx.client.OlxApiClient;
import ba.autovendor.backend.olx.client.dto.OlxImageDto;
import ba.autovendor.backend.woo.client.dto.WooPluginImageDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Transfers WooCommerce product images to an OLX listing via the `image_url` upload
 * (OLX fetches the remote URL itself — no local download needed). A single image
 * failure is collected, not fatal: the sync still publishes a partially-illustrated
 * (or text-only) listing, and the failures end up in the sync log message.
 */
@Component
public class ImagePipeline {

    private final OlxApiClient olxApiClient;

    public ImagePipeline(OlxApiClient olxApiClient) {
        this.olxApiClient = olxApiClient;
    }

    /** Uploads all images, sets the first uploaded one as main; returns per-image error notes. */
    public List<String> transfer(String token, long listingId, List<WooPluginImageDto> images) {
        List<String> errors = new ArrayList<>();
        if (images == null || images.isEmpty()) {
            return errors;
        }

        Long mainImageId = null;
        for (WooPluginImageDto image : images) {
            if (image.src() == null || image.src().isBlank()) {
                continue;
            }
            try {
                List<OlxImageDto> uploaded = olxApiClient.uploadImageByUrl(token, listingId, image.src());
                if (mainImageId == null && uploaded != null && !uploaded.isEmpty()
                        && uploaded.getFirst().id() != null) {
                    mainImageId = uploaded.getFirst().id();
                }
            } catch (Exception e) {
                errors.add("Image upload failed (" + image.src() + "): " + e.getMessage());
            }
        }

        if (mainImageId != null) {
            try {
                olxApiClient.setMainImage(token, listingId, mainImageId);
            } catch (Exception e) {
                errors.add("Setting main image failed: " + e.getMessage());
            }
        }
        return errors;
    }
}
