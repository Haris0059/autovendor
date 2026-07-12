package ba.autovendor.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduling is opt-in via {@code app.sync.scheduling-enabled} so integration
 * tests (which disable it centrally in TestcontainersConfiguration) never have
 * background sweeps racing their fixtures.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.sync.scheduling-enabled", havingValue = "true")
public class SchedulingConfig {
}
