package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.shared.spi.analytics.CommerceAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CommerceAnalyticsSnapshot;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommerceAnalyticsServiceImpl implements CommerceAnalyticsService {

    private final CommercePurchaseRepository commercePurchaseRepository;
    private final CommercePurchaseItemRepository commercePurchaseItemRepository;

    @Override
    public CommerceAnalyticsSnapshot captureSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        long totalOrders = commercePurchaseRepository.count();
        long ordersLast30Days = commercePurchaseRepository.countByCreatedDateAfter(thirtyDaysAgo);
        long capturedOrders = commercePurchaseRepository.countByPaymentStatusIgnoreCase("captured");
        long uniqueCustomers = commercePurchaseRepository.countDistinctCustomers();
        long newCustomersLast30Days = commercePurchaseRepository.countDistinctCustomersCreatedAfter(thirtyDaysAgo);
        long coursePurchasesLast30Days = commercePurchaseItemRepository
                .countByScopeAndCreatedDateAfter(PurchaseScope.COURSE, thirtyDaysAgo);
        long classPurchasesLast30Days = commercePurchaseItemRepository
                .countByScopeAndCreatedDateAfter(PurchaseScope.CLASS, thirtyDaysAgo);

        return new CommerceAnalyticsSnapshot(
                totalOrders,
                ordersLast30Days,
                capturedOrders,
                uniqueCustomers,
                newCustomersLast30Days,
                coursePurchasesLast30Days,
                classPurchasesLast30Days
        );
    }
}
