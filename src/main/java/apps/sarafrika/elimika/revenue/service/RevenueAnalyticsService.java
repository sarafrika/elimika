package apps.sarafrika.elimika.revenue.service;

import apps.sarafrika.elimika.revenue.dto.RevenueAnalyticsOverviewDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueDashboardDTO;
import apps.sarafrika.elimika.revenue.dto.RevenuePaymentDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueSaleLineItemDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueAmountDTO;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RevenueAnalyticsService {

    RevenueDashboardDTO getRevenueDashboard(UserDomain domain, LocalDate startDate, LocalDate endDate);

    RevenueAnalyticsOverviewDTO getAnalyticsOverview(LocalDate startDate, LocalDate endDate);

    Page<RevenueSaleLineItemDTO> getSales(
            UserDomain domain,
            LocalDate startDate,
            LocalDate endDate,
            String paymentStatus,
            String scope,
            java.util.UUID courseUuid,
            java.util.UUID classDefinitionUuid,
            java.util.UUID studentUuid,
            Pageable pageable
    );

    Page<RevenuePaymentDTO> getPayments(
            UserDomain domain,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String orderId,
            Pageable pageable
    );

    List<RevenueAmountDTO> getPlatformFeeSummary(LocalDate startDate, LocalDate endDate);
}
