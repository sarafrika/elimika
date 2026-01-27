package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.commerce.purchase.spi.CommercePlatformFeeSummary;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueQueryService;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceSaleLineItemView;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommerceRevenueQueryServiceImpl implements CommerceRevenueQueryService {

    private final CommercePurchaseItemRepository purchaseItemRepository;
    private final CommercePurchaseRepository purchaseRepository;

    @Override
    public List<CommerceRevenueLineItem> findCapturedRevenueLines(OffsetDateTime startDate, OffsetDateTime endDate) {
        return purchaseItemRepository.findCapturedRevenueLines(startDate, endDate);
    }

    @Override
    public List<CommerceRevenueLineItem> findCapturedRevenueLinesByCourseUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> courseUuids
    ) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return List.of();
        }
        return purchaseItemRepository.findCapturedRevenueLinesByCourseUuids(startDate, endDate, courseUuids);
    }

    @Override
    public List<CommerceRevenueLineItem> findCapturedRevenueLinesByClassDefinitionUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> classDefinitionUuids
    ) {
        if (classDefinitionUuids == null || classDefinitionUuids.isEmpty()) {
            return List.of();
        }
        return purchaseItemRepository.findCapturedRevenueLinesByClassDefinitionUuids(
                startDate,
                endDate,
                classDefinitionUuids
        );
    }

    @Override
    public List<CommerceRevenueLineItem> findCapturedRevenueLinesByStudentUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> studentUuids
    ) {
        if (studentUuids == null || studentUuids.isEmpty()) {
            return List.of();
        }
        return purchaseItemRepository.findCapturedRevenueLinesByStudentUuids(startDate, endDate, studentUuids);
    }

    @Override
    public Page<CommerceSaleLineItemView> findSales(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            Pageable pageable
    ) {
        return purchaseItemRepository.findSales(startDate, endDate, paymentStatus, scope, pageable);
    }

    @Override
    public Page<CommerceSaleLineItemView> findSalesByCourseUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            List<UUID> courseUuids,
            Pageable pageable
    ) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return Page.empty(pageable);
        }
        return purchaseItemRepository.findSalesByCourseUuids(startDate, endDate, paymentStatus, scope, courseUuids, pageable);
    }

    @Override
    public Page<CommerceSaleLineItemView> findSalesByClassDefinitionUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            List<UUID> classDefinitionUuids,
            Pageable pageable
    ) {
        if (classDefinitionUuids == null || classDefinitionUuids.isEmpty()) {
            return Page.empty(pageable);
        }
        return purchaseItemRepository.findSalesByClassDefinitionUuids(
                startDate,
                endDate,
                paymentStatus,
                scope,
                classDefinitionUuids,
                pageable
        );
    }

    @Override
    public Page<CommerceSaleLineItemView> findSalesByStudentUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String paymentStatus,
            PurchaseScope scope,
            List<UUID> studentUuids,
            Pageable pageable
    ) {
        if (studentUuids == null || studentUuids.isEmpty()) {
            return Page.empty(pageable);
        }
        return purchaseItemRepository.findSalesByStudentUuids(startDate, endDate, paymentStatus, scope, studentUuids, pageable);
    }

    @Override
    public List<CommercePlatformFeeSummary> summarizePlatformFees(OffsetDateTime startDate, OffsetDateTime endDate) {
        return purchaseRepository.summarizePlatformFees(startDate, endDate);
    }

    @Override
    public boolean orderBelongsToCourseUuids(String orderId, List<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return false;
        }
        return purchaseItemRepository.existsOrderForCourseUuids(orderId, courseUuids);
    }

    @Override
    public boolean orderBelongsToClassDefinitionUuids(String orderId, List<UUID> classDefinitionUuids) {
        if (classDefinitionUuids == null || classDefinitionUuids.isEmpty()) {
            return false;
        }
        return purchaseItemRepository.existsOrderForClassDefinitionUuids(orderId, classDefinitionUuids);
    }

    @Override
    public boolean orderBelongsToStudentUuids(String orderId, List<UUID> studentUuids) {
        if (studentUuids == null || studentUuids.isEmpty()) {
            return false;
        }
        return purchaseItemRepository.existsOrderForStudentUuids(orderId, studentUuids);
    }
}
