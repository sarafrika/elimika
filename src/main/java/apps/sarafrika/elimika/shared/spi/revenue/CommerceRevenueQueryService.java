package apps.sarafrika.elimika.shared.spi.revenue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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

    /**
     * Commerce totals for a single course, aggregated in the database.
     * <p>
     * The course statistics endpoint shows these only to the course's creator and to platform
     * admins, so the figures are summed here rather than shipped as line items a caller could
     * re-read at a different scope.
     *
     * @param courseUuid the course to total
     * @return the summary, all zeros when the course has never sold
     */
    CourseSalesSummary summariseSalesForCourse(UUID courseUuid);

    /**
     * What was credited to earners for captured sales of the given classes.
     * <p>
     * This is the trainer-facing figure: money that actually reached a wallet for their own classes,
     * not the gross the learner paid.
     *
     * @param classDefinitionUuids the classes to total across; empty yields zero
     * @return the total credited, never null
     */
    java.math.BigDecimal sumCapturedCreditsForClassDefinitions(java.util.Collection<UUID> classDefinitionUuids);

    List<CommercePlatformFeeSummary> summarizePlatformFees(OffsetDateTime startDate, OffsetDateTime endDate);

    boolean orderBelongsToCourseUuids(String orderId, List<UUID> courseUuids);

    boolean orderBelongsToClassDefinitionUuids(String orderId, List<UUID> classDefinitionUuids);

    boolean orderBelongsToStudentUuids(String orderId, List<UUID> studentUuids);
}
