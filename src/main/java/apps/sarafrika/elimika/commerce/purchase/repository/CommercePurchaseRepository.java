package apps.sarafrika.elimika.commerce.purchase.repository;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommercePurchaseRepository extends JpaRepository<CommercePurchase, Long> {

    Optional<CommercePurchase> findByMedusaOrderId(String medusaOrderId);
}
