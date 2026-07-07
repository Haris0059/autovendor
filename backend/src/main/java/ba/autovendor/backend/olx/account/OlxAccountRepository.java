package ba.autovendor.backend.olx.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OlxAccountRepository extends JpaRepository<OlxAccount, Long> {

    List<OlxAccount> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<OlxAccount> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndUsername(Long userId, String username);
}
