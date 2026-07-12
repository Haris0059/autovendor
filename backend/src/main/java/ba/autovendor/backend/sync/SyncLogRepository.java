package ba.autovendor.backend.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long>, JpaSpecificationExecutor<SyncLog> {

    Optional<SyncLog> findFirstByProductLinkIdOrderByIdDesc(Long productLinkId);
}
