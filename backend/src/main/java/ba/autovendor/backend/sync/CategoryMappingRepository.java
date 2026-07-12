package ba.autovendor.backend.sync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryMappingRepository extends JpaRepository<CategoryMapping, Long> {

    List<CategoryMapping> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CategoryMapping> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndWooCategoryId(Long userId, Long wooCategoryId);

    Optional<CategoryMapping> findByUserIdAndWooCategoryId(Long userId, Long wooCategoryId);
}
