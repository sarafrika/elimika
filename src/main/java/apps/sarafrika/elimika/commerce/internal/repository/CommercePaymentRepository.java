package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommercePayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommercePaymentRepository extends JpaRepository<CommercePayment, Long> {
}
