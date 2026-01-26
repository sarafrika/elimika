package apps.sarafrika.elimika.commerce.purchase.repository;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommercePurchaseItemRepository extends JpaRepository<CommercePurchaseItem, Long> {

    List<CommercePurchaseItem> findByStudentUuidAndScopeAndCourseUuid(UUID studentUuid, PurchaseScope scope, UUID courseUuid);

    List<CommercePurchaseItem> findByStudentUuidAndScopeAndClassDefinitionUuid(UUID studentUuid, PurchaseScope scope, UUID classDefinitionUuid);

    long countByScope(PurchaseScope scope);

    long countByScopeAndCreatedDateAfter(PurchaseScope scope, LocalDateTime createdDate);

    @Query("""
            select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem(
                p.orderId,
                p.orderCreatedAt,
                p.orderCurrencyCode,
                i.total,
                i.quantity,
                i.scope,
                i.courseUuid,
                i.classDefinitionUuid
            )
            from CommercePurchaseItem i
            join i.purchase p
            where lower(p.paymentStatus) = 'captured'
              and p.orderCreatedAt >= :startDate
              and p.orderCreatedAt <= :endDate
            """)
    List<CommerceRevenueLineItem> findCapturedRevenueLines(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
            select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem(
                p.orderId,
                p.orderCreatedAt,
                p.orderCurrencyCode,
                i.total,
                i.quantity,
                i.scope,
                i.courseUuid,
                i.classDefinitionUuid
            )
            from CommercePurchaseItem i
            join i.purchase p
            where lower(p.paymentStatus) = 'captured'
              and p.orderCreatedAt >= :startDate
              and p.orderCreatedAt <= :endDate
              and i.courseUuid in :courseUuids
            """)
    List<CommerceRevenueLineItem> findCapturedRevenueLinesByCourseUuids(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("courseUuids") List<UUID> courseUuids
    );

    @Query("""
            select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem(
                p.orderId,
                p.orderCreatedAt,
                p.orderCurrencyCode,
                i.total,
                i.quantity,
                i.scope,
                i.courseUuid,
                i.classDefinitionUuid
            )
            from CommercePurchaseItem i
            join i.purchase p
            where lower(p.paymentStatus) = 'captured'
              and p.orderCreatedAt >= :startDate
              and p.orderCreatedAt <= :endDate
              and i.classDefinitionUuid in :classDefinitionUuids
            """)
    List<CommerceRevenueLineItem> findCapturedRevenueLinesByClassDefinitionUuids(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("classDefinitionUuids") List<UUID> classDefinitionUuids
    );

    @Query("""
            select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem(
                p.orderId,
                p.orderCreatedAt,
                p.orderCurrencyCode,
                i.total,
                i.quantity,
                i.scope,
                i.courseUuid,
                i.classDefinitionUuid
            )
            from CommercePurchaseItem i
            join i.purchase p
            where lower(p.paymentStatus) = 'captured'
              and p.orderCreatedAt >= :startDate
              and p.orderCreatedAt <= :endDate
              and i.studentUuid in :studentUuids
            """)
    List<CommerceRevenueLineItem> findCapturedRevenueLinesByStudentUuids(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("studentUuids") List<UUID> studentUuids
    );
}
