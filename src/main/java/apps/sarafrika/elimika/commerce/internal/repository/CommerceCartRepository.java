package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceCart;
import apps.sarafrika.elimika.commerce.internal.enums.CartStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommerceCartRepository extends JpaRepository<CommerceCart, Long> {

    Optional<CommerceCart> findByUuid(UUID uuid);

    List<CommerceCart> findByStatusAndExpiresAtBefore(CartStatus status, LocalDateTime expiresAt);
}
