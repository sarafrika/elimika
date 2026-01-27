package apps.sarafrika.elimika.commerce.purchase.spi;

import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommerceRevenueQueryService {

    List<CommerceRevenueLineItem> findCapturedRevenueLines(OffsetDateTime startDate, OffsetDateTime endDate);

    List<CommerceRevenueLineItem> findCapturedRevenueLinesByCourseUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> courseUuids
    );

    List<CommerceRevenueLineItem> findCapturedRevenueLinesByClassDefinitionUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> classDefinitionUuids
    );

    List<CommerceRevenueLineItem> findCapturedRevenueLinesByStudentUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> studentUuids
    );

    Page<CommerceSaleLineItemView> findSales(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            Pageable pageable
    );

    Page<CommerceSaleLineItemView> findSalesByCourseUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            List<UUID> courseUuids,
            Pageable pageable
    );

    Page<CommerceSaleLineItemView> findSalesByClassDefinitionUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            List<UUID> classDefinitionUuids,
            Pageable pageable
    );

    Page<CommerceSaleLineItemView> findSalesByStudentUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            List<UUID> studentUuids,
            Pageable pageable
    );

    List<CommercePlatformFeeSummary> summarizePlatformFees(OffsetDateTime startDate, OffsetDateTime endDate);

    boolean orderBelongsToCourseUuids(String orderId, List<UUID> courseUuids);

    boolean orderBelongsToClassDefinitionUuids(String orderId, List<UUID> classDefinitionUuids);

    boolean orderBelongsToStudentUuids(String orderId, List<UUID> studentUuids);
}
