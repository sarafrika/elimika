package apps.sarafrika.elimika.commerce.purchase.repository;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceSaleLineItemView;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query(
            value = """
                select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceSaleLineItemView(
                    p.orderId,
                    p.orderNumber,
                    p.orderCreatedAt,
                    p.paymentStatus,
                    p.orderCurrencyCode,
                    p.orderSubtotalAmount,
                    p.orderTotalAmount,
                    p.platformFeeAmount,
                    p.platformFeeCurrency,
                    p.platformFeeRuleUuid,
                    p.userUuid,
                    p.customerEmail,
                    i.lineItemId,
                    i.variantId,
                    i.title,
                    i.quantity,
                    i.unitPrice,
                    i.subtotal,
                    i.total,
                    i.scope,
                    i.courseUuid,
                    i.classDefinitionUuid,
                    i.studentUuid
                )
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                """,
            countQuery = """
                select count(i)
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                """
    )
    Page<CommerceSaleLineItemView> findSales(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("paymentStatus") String paymentStatus,
            @Param("scope") PurchaseScope scope,
            Pageable pageable
    );

    @Query(
            value = """
                select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceSaleLineItemView(
                    p.orderId,
                    p.orderNumber,
                    p.orderCreatedAt,
                    p.paymentStatus,
                    p.orderCurrencyCode,
                    p.orderSubtotalAmount,
                    p.orderTotalAmount,
                    p.platformFeeAmount,
                    p.platformFeeCurrency,
                    p.platformFeeRuleUuid,
                    p.userUuid,
                    p.customerEmail,
                    i.lineItemId,
                    i.variantId,
                    i.title,
                    i.quantity,
                    i.unitPrice,
                    i.subtotal,
                    i.total,
                    i.scope,
                    i.courseUuid,
                    i.classDefinitionUuid,
                    i.studentUuid
                )
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                  and i.courseUuid in :courseUuids
                """,
            countQuery = """
                select count(i)
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                  and i.courseUuid in :courseUuids
                """
    )
    Page<CommerceSaleLineItemView> findSalesByCourseUuids(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("paymentStatus") String paymentStatus,
            @Param("scope") PurchaseScope scope,
            @Param("courseUuids") List<UUID> courseUuids,
            Pageable pageable
    );

    @Query(
            value = """
                select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceSaleLineItemView(
                    p.orderId,
                    p.orderNumber,
                    p.orderCreatedAt,
                    p.paymentStatus,
                    p.orderCurrencyCode,
                    p.orderSubtotalAmount,
                    p.orderTotalAmount,
                    p.platformFeeAmount,
                    p.platformFeeCurrency,
                    p.platformFeeRuleUuid,
                    p.userUuid,
                    p.customerEmail,
                    i.lineItemId,
                    i.variantId,
                    i.title,
                    i.quantity,
                    i.unitPrice,
                    i.subtotal,
                    i.total,
                    i.scope,
                    i.courseUuid,
                    i.classDefinitionUuid,
                    i.studentUuid
                )
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                  and i.classDefinitionUuid in :classDefinitionUuids
                """,
            countQuery = """
                select count(i)
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                  and i.classDefinitionUuid in :classDefinitionUuids
                """
    )
    Page<CommerceSaleLineItemView> findSalesByClassDefinitionUuids(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("paymentStatus") String paymentStatus,
            @Param("scope") PurchaseScope scope,
            @Param("classDefinitionUuids") List<UUID> classDefinitionUuids,
            Pageable pageable
    );

    @Query(
            value = """
                select new apps.sarafrika.elimika.commerce.purchase.spi.CommerceSaleLineItemView(
                    p.orderId,
                    p.orderNumber,
                    p.orderCreatedAt,
                    p.paymentStatus,
                    p.orderCurrencyCode,
                    p.orderSubtotalAmount,
                    p.orderTotalAmount,
                    p.platformFeeAmount,
                    p.platformFeeCurrency,
                    p.platformFeeRuleUuid,
                    p.userUuid,
                    p.customerEmail,
                    i.lineItemId,
                    i.variantId,
                    i.title,
                    i.quantity,
                    i.unitPrice,
                    i.subtotal,
                    i.total,
                    i.scope,
                    i.courseUuid,
                    i.classDefinitionUuid,
                    i.studentUuid
                )
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                  and i.studentUuid in :studentUuids
                """,
            countQuery = """
                select count(i)
                from CommercePurchaseItem i
                join i.purchase p
                where p.orderCreatedAt >= :startDate
                  and p.orderCreatedAt <= :endDate
                  and (:paymentStatus is null or lower(p.paymentStatus) = lower(:paymentStatus))
                  and (:scope is null or i.scope = :scope)
                  and i.studentUuid in :studentUuids
                """
    )
    Page<CommerceSaleLineItemView> findSalesByStudentUuids(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("paymentStatus") String paymentStatus,
            @Param("scope") PurchaseScope scope,
            @Param("studentUuids") List<UUID> studentUuids,
            Pageable pageable
    );

    @Query("""
            select case when count(i) > 0 then true else false end
            from CommercePurchaseItem i
            join i.purchase p
            where p.orderId = :orderId
              and i.courseUuid in :courseUuids
            """)
    boolean existsOrderForCourseUuids(
            @Param("orderId") String orderId,
            @Param("courseUuids") List<UUID> courseUuids
    );

    @Query("""
            select case when count(i) > 0 then true else false end
            from CommercePurchaseItem i
            join i.purchase p
            where p.orderId = :orderId
              and i.classDefinitionUuid in :classDefinitionUuids
            """)
    boolean existsOrderForClassDefinitionUuids(
            @Param("orderId") String orderId,
            @Param("classDefinitionUuids") List<UUID> classDefinitionUuids
    );

    @Query("""
            select case when count(i) > 0 then true else false end
            from CommercePurchaseItem i
            join i.purchase p
            where p.orderId = :orderId
              and i.studentUuid in :studentUuids
            """)
    boolean existsOrderForStudentUuids(
            @Param("orderId") String orderId,
            @Param("studentUuids") List<UUID> studentUuids
    );
}
