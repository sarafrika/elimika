package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceCartItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommerceCartItemRepository extends JpaRepository<CommerceCartItem, Long> {

    void deleteByUuid(UUID uuid);

    Optional<CommerceCartItem> findByUuid(UUID uuid);
}
