package ba.autovendor.backend.olx.sponsor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SponsorshipRepository extends JpaRepository<Sponsorship, Long> {

    List<Sponsorship> findAllByUserIdAndEndedAtIsNullAndEndsAtAfterOrderByStartedAtDesc(
            Long userId, OffsetDateTime now);

    Optional<Sponsorship> findByIdAndUserId(Long id, Long userId);

    List<Sponsorship> findAllByOlxListingIdAndEndedAtIsNull(Long olxListingId);
}
