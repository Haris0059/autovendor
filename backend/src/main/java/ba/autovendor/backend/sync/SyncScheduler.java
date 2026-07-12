package ba.autovendor.backend.sync;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Thin shell over {@link BulkSyncService}: the loops and error handling live in
 * the service so tests exercise them without scheduling. Whether these methods
 * fire at all is controlled by {@code app.sync.scheduling-enabled} (the
 * conditional {@code @EnableScheduling} in SchedulingConfig) — disabled in tests.
 */
@Component
public class SyncScheduler {

    /** Proactive re-login margin; must exceed the token manager's 5-minute lazy skew. */
    private static final Duration TOKEN_REFRESH_WINDOW = Duration.ofMinutes(30);

    private final BulkSyncService bulkSyncService;

    public SyncScheduler(BulkSyncService bulkSyncService) {
        this.bulkSyncService = bulkSyncService;
    }

    @Scheduled(fixedDelayString = "${app.sync.job-interval:PT10M}")
    public void sweep() {
        bulkSyncService.sweepAllStores();
    }

    @Scheduled(fixedDelayString = "${app.sync.token-refresh-interval:PT1H}")
    public void refreshTokens() {
        bulkSyncService.refreshExpiringTokens(TOKEN_REFRESH_WINDOW);
    }
}
