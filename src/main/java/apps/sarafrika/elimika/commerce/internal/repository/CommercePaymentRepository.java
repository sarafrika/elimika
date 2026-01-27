package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommercePayment;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommercePaymentRepository extends JpaRepository<CommercePayment, Long> {
    @EntityGraph(attributePaths = "order")
    @Query("""
            select p from CommercePayment p
            where (:startDate is null or p.processedAt >= :startDate)
              and (:endDate is null or p.processedAt <= :endDate)
            """)
    Page<CommercePayment> findByProcessedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "order")
    @Query("""
            select p from CommercePayment p
            where (:startDate is null or p.processedAt >= :startDate)
              and (:endDate is null or p.processedAt <= :endDate)
              and p.status = :status
            """)
    Page<CommercePayment> findByProcessedAtBetweenAndStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "order")
    @Query("""
            select p from CommercePayment p
            where p.order.uuid in :orderUuids
              and (:startDate is null or p.processedAt >= :startDate)
              and (:endDate is null or p.processedAt <= :endDate)
            """)
    Page<CommercePayment> findByOrderUuids(
            @Param("orderUuids") List<UUID> orderUuids,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "order")
    @Query("""
            select p from CommercePayment p
            where p.order.uuid in :orderUuids
              and (:startDate is null or p.processedAt >= :startDate)
              and (:endDate is null or p.processedAt <= :endDate)
              and p.status = :status
            """)
    Page<CommercePayment> findByOrderUuidsAndStatus(
            @Param("orderUuids") List<UUID> orderUuids,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );
}
