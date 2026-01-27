package apps.sarafrika.elimika.commerce.purchase.repository;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.spi.CommercePlatformFeeSummary;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommercePurchaseRepository extends JpaRepository<CommercePurchase, Long> {

    Optional<CommercePurchase> findByOrderId(String orderId);

    long countByCreatedDateAfter(LocalDateTime createdDate);

    long countByPaymentStatusIgnoreCase(String paymentStatus);

    @Query("SELECT COUNT(DISTINCT cp.customerEmail) FROM CommercePurchase cp WHERE cp.customerEmail IS NOT NULL")
    long countDistinctCustomers();

    @Query("SELECT COUNT(DISTINCT cp.customerEmail) FROM CommercePurchase cp WHERE cp.customerEmail IS NOT NULL AND cp.createdDate >= :createdAfter")
    long countDistinctCustomersCreatedAfter(LocalDateTime createdAfter);

    @Query("""
            select new apps.sarafrika.elimika.commerce.purchase.spi.CommercePlatformFeeSummary(
                cp.platformFeeCurrency,
                sum(cp.platformFeeAmount)
            )
            from CommercePurchase cp
            where cp.orderCreatedAt >= :startDate
              and cp.orderCreatedAt <= :endDate
              and cp.platformFeeAmount is not null
              and cp.platformFeeCurrency is not null
              and lower(cp.paymentStatus) = 'captured'
            group by cp.platformFeeCurrency
            """)
    List<CommercePlatformFeeSummary> summarizePlatformFees(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
