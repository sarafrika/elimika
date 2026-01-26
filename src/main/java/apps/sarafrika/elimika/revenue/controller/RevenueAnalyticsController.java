package apps.sarafrika.elimika.revenue.controller;

import apps.sarafrika.elimika.revenue.dto.RevenueDashboardDTO;
import apps.sarafrika.elimika.revenue.service.RevenueAnalyticsService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/revenue")
@Tag(name = "Revenue Analytics", description = "Revenue insights for domain dashboards")
@RequiredArgsConstructor
public class RevenueAnalyticsController {

    private final RevenueAnalyticsService revenueAnalyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("@domainSecurityService.hasAnyDomain(T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).student, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).instructor, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).admin, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).parent, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).organisation_user, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).course_creator)")
    @Operation(summary = "Get revenue dashboard analytics")
    public ResponseEntity<ApiResponse<RevenueDashboardDTO>> getRevenueDashboard(
            @RequestParam("domain") UserDomain domain,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start_date must be on or before end_date");
        }
        RevenueDashboardDTO dashboard = revenueAnalyticsService.getRevenueDashboard(domain, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Revenue dashboard retrieved successfully"));
    }
}
