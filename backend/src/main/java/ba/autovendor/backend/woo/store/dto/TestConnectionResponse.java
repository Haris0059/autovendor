package ba.autovendor.backend.woo.store.dto;

public record TestConnectionResponse(
        boolean ok,
        int productsCount
) {
}
