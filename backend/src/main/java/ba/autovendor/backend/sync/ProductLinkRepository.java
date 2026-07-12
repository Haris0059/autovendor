package ba.autovendor.backend.sync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductLinkRepository extends JpaRepository<ProductLink, Long> {

    List<ProductLink> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ProductLink> findByIdAndUserId(Long id, Long userId);

    boolean existsByWooStoreIdAndWooProductId(Long wooStoreId, Long wooProductId);
}
