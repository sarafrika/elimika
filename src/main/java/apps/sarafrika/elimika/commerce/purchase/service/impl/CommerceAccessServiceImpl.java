package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class CommerceAccessServiceImpl implements CommerceAccessService {

    private static final Set<String> SUCCESS_STATUSES = Set.of(
            "captured",
            "paid",
            "authorized",
            "partially_captured",
            "CAPTURED",
            "PAID",
            "AUTHORIZED",
            "PARTIALLY_CAPTURED"
    );

    private final CommercePurchaseItemRepository purchaseItemRepository;

    @Override
    public boolean hasCourseAccess(UUID studentUuid, UUID courseUuid) {
        if (studentUuid == null || courseUuid == null) {
            return false;
        }
        List<CommercePurchaseItem> items =
                purchaseItemRepository.findByStudentUuidAndScopeAndCourseUuid(studentUuid, PurchaseScope.COURSE, courseUuid);
        return hasSettledPurchase(items);
    }

    @Override
    public boolean hasClassAccess(UUID studentUuid, UUID classDefinitionUuid) {
        if (studentUuid == null || classDefinitionUuid == null) {
            return false;
        }
        List<CommercePurchaseItem> items = purchaseItemRepository.findByStudentUuidAndScopeAndClassDefinitionUuid(
                studentUuid, PurchaseScope.CLASS, classDefinitionUuid);
        return hasSettledPurchase(items);
    }

    private boolean hasSettledPurchase(List<CommercePurchaseItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return false;
        }
        return items.stream().anyMatch(this::isSettled);
    }

    private boolean isSettled(CommercePurchaseItem item) {
        String paymentStatus = item.getPurchase() != null ? item.getPurchase().getPaymentStatus() : null;
        if (paymentStatus == null) {
            return false;
        }
        return SUCCESS_STATUSES.contains(paymentStatus.toLowerCase());
    }
}
