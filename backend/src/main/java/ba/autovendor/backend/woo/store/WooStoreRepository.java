package ba.autovendor.backend.woo.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WooStoreRepository extends JpaRepository<WooStore, Long> {

    List<WooStore> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WooStore> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndStoreUrl(Long userId, String storeUrl);

    List<WooStore> findAllByStoreUrl(String storeUrl);
}
