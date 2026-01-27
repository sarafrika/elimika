package apps.sarafrika.elimika.revenue.controller;

import apps.sarafrika.elimika.revenue.dto.RevenueAnalyticsOverviewDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueDashboardDTO;
import apps.sarafrika.elimika.revenue.dto.RevenuePaymentDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueSaleLineItemDTO;
import apps.sarafrika.elimika.revenue.dto.RevenueAmountDTO;
import apps.sarafrika.elimika.revenue.service.RevenueAnalyticsService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/analytics/revenue")
@Tag(name = "Revenue Analytics", description = "Revenue insights for domain analytics")
@RequiredArgsConstructor
public class RevenueAnalyticsController {

    private final RevenueAnalyticsService revenueAnalyticsService;

    @GetMapping
    @PreAuthorize("@domainSecurityService.hasAnyDomain(T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).student, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).instructor, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).admin, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).parent, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).organisation_user, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).course_creator)")
    @Operation(summary = "Get revenue analytics for all domains of the current user")
    public ResponseEntity<ApiResponse<RevenueAnalyticsOverviewDTO>> getAnalyticsOverview(
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        RevenueAnalyticsOverviewDTO overview = revenueAnalyticsService.getAnalyticsOverview(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(overview, "Revenue analytics retrieved successfully"));
    }

    @GetMapping("/domain")
    @PreAuthorize("@domainSecurityService.hasAnyDomain(T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).student, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).instructor, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).admin, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).parent, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).organisation_user, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).course_creator)")
    @Operation(summary = "Get revenue analytics for a specific domain")
    public ResponseEntity<ApiResponse<RevenueDashboardDTO>> getRevenueDashboard(
            @RequestParam("domain") UserDomain domain,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        RevenueDashboardDTO dashboard = revenueAnalyticsService.getRevenueDashboard(domain, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Revenue analytics retrieved successfully"));
    }

    @GetMapping("/sales")
    @PreAuthorize("@domainSecurityService.hasAnyDomain(T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).student, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).instructor, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).admin, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).parent, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).organisation_user, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).course_creator)")
    @Operation(summary = "List sales line items for revenue analytics")
    public ResponseEntity<ApiResponse<PagedDTO<RevenueSaleLineItemDTO>>> listSales(
            @RequestParam("domain") UserDomain domain,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "payment_status", required = false) String paymentStatus,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "course_uuid", required = false) UUID courseUuid,
            @RequestParam(value = "class_definition_uuid", required = false) UUID classDefinitionUuid,
            @RequestParam(value = "student_uuid", required = false) UUID studentUuid,
            Pageable pageable
    ) {
        validateDateRange(startDate, endDate);
        Page<RevenueSaleLineItemDTO> sales = revenueAnalyticsService.getSales(
                domain,
                startDate,
                endDate,
                paymentStatus,
                scope,
                courseUuid,
                classDefinitionUuid,
                studentUuid,
                pageable
        );
        PagedDTO<RevenueSaleLineItemDTO> paged = PagedDTO.from(
                sales,
                ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(paged, "Revenue sales retrieved successfully"));
    }

    @GetMapping("/payments")
    @PreAuthorize("@domainSecurityService.hasAnyDomain(T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).student, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).instructor, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).admin, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).parent, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).organisation_user, " +
            "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain).course_creator)")
    @Operation(summary = "List payment transactions for revenue analytics")
    public ResponseEntity<ApiResponse<PagedDTO<RevenuePaymentDTO>>> listPayments(
            @RequestParam("domain") UserDomain domain,
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "order_id", required = false) String orderId,
            Pageable pageable
    ) {
        validateDateRange(startDate, endDate);
        Page<RevenuePaymentDTO> payments = revenueAnalyticsService.getPayments(
                domain,
                startDate,
                endDate,
                status,
                orderId,
                pageable
        );
        PagedDTO<RevenuePaymentDTO> paged = PagedDTO.from(
                payments,
                ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(paged, "Revenue payments retrieved successfully"));
    }

    @GetMapping("/platform-fees/summary")
    @PreAuthorize("@domainSecurityService.isOrganizationAdmin()")
    @Operation(summary = "Summarize platform fees withheld")
    public ResponseEntity<ApiResponse<List<RevenueAmountDTO>>> getPlatformFeeSummary(
            @RequestParam(value = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        List<RevenueAmountDTO> summary = revenueAnalyticsService.getPlatformFeeSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary, "Platform fee summary retrieved successfully"));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start_date must be on or before end_date");
        }
    }
}
