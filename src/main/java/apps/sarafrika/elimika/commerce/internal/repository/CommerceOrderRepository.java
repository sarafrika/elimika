package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.enums.OrderStatus;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, Long> {

    Optional<CommerceOrder> findByUuid(UUID uuid);

    List<CommerceOrder> findByStatus(OrderStatus status);

    /**
     * Orders whose M-Pesa prompt was issued but never resolved in the browser. The sweep re-asks the
     * gateway about these, because a learner who closed the tab has still paid.
     */
    List<CommerceOrder> findByPaymentStatusAndCheckoutRequestIdIsNotNullAndPlacedAtBefore(
            PaymentStatus paymentStatus, java.time.LocalDateTime placedBefore);
}
