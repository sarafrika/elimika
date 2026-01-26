package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueLineItem;
import apps.sarafrika.elimika.commerce.purchase.spi.CommerceRevenueQueryService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommerceRevenueQueryServiceImpl implements CommerceRevenueQueryService {

    private final CommercePurchaseItemRepository purchaseItemRepository;

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
}
