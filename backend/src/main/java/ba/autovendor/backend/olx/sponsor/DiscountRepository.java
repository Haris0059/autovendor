package ba.autovendor.backend.olx.sponsor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findAllByUserIdAndEndedAtIsNullAndEndsAtAfterOrderByStartedAtDesc(
            Long userId, OffsetDateTime now);

    Optional<Discount> findByIdAndUserId(Long id, Long userId);

    List<Discount> findAllByOlxListingIdAndEndedAtIsNull(Long olxListingId);
}
