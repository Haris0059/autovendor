package ba.autovendor.backend.woo.client.dto;

import java.util.List;

public record WooHashPageDto(
        List<ProductHash> products,
        Integer page,
        Integer perPage,
        Integer count
) {
    public record ProductHash(Long id, String hash) {
    }
}
