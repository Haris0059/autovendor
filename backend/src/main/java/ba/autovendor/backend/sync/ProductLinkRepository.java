package ba.autovendor.backend.sync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductLinkRepository extends JpaRepository<ProductLink, Long> {

    List<ProductLink> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<ProductLink> findAllByWooStoreId(Long wooStoreId);

    Optional<ProductLink> findByWooStoreIdAndWooProductId(Long wooStoreId, Long wooProductId);

    Optional<ProductLink> findByIdAndUserId(Long id, Long userId);

    boolean existsByWooStoreIdAndWooProductId(Long wooStoreId, Long wooProductId);
}
