package ba.autovendor.backend.sync;

/** Lowercase constants on purpose — see {@link SyncDirection} for the rationale. */
public enum SyncStatus {
    success,
    failed,
    skipped,
    pending
}
