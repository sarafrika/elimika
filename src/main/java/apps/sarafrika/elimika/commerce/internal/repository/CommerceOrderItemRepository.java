package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommerceOrderItemRepository extends JpaRepository<CommerceOrderItem, Long> {
}
