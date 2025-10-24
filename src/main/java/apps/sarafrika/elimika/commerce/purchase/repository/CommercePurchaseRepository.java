package apps.sarafrika.elimika.commerce.purchase.repository;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommercePurchaseRepository extends JpaRepository<CommercePurchase, Long> {

    Optional<CommercePurchase> findByMedusaOrderId(String medusaOrderId);

    long countByCreatedDateAfter(LocalDateTime createdDate);

    long countByPaymentStatusIgnoreCase(String paymentStatus);

    @Query("SELECT COUNT(DISTINCT cp.customerEmail) FROM CommercePurchase cp WHERE cp.customerEmail IS NOT NULL")
    long countDistinctCustomers();

    @Query("SELECT COUNT(DISTINCT cp.customerEmail) FROM CommercePurchase cp WHERE cp.customerEmail IS NOT NULL AND cp.createdDate >= :createdAfter")
    long countDistinctCustomersCreatedAfter(LocalDateTime createdAfter);
}
