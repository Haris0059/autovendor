package ba.autovendor.backend.sync;

/**
 * Constants are deliberately lowercase snake_case: with {@code @Enumerated(STRING)} the
 * same literal flows through the DB column, JSON bodies/responses and query params with
 * no Jackson or converter configuration. Only {@link #woo_to_olx} is synced today; the
 * other directions are stored for forward compatibility and skipped by the engine.
 */
public enum SyncDirection {
    woo_to_olx,
    olx_to_woo,
    bidirectional
}
