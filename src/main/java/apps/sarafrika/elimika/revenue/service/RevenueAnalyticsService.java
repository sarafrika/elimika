package apps.sarafrika.elimika.revenue.service;

import apps.sarafrika.elimika.revenue.dto.RevenueDashboardDTO;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import java.time.LocalDate;

public interface RevenueAnalyticsService {

    RevenueDashboardDTO getRevenueDashboard(UserDomain domain, LocalDate startDate, LocalDate endDate);
}
