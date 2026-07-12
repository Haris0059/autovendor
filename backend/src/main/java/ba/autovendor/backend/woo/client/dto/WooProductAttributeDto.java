package ba.autovendor.backend.woo.client.dto;

import java.util.List;

/**
 * Per-product attribute from the plugin's map_product: {@code name} is the raw
 * taxonomy (e.g. "pa_vrsta"), {@code label} the human name, {@code options} the
 * product's value(s) — several for variable products.
 */
public record WooProductAttributeDto(
        String name,
        String label,
        List<String> options,
        Boolean visible,
        Boolean variation
) {
}
