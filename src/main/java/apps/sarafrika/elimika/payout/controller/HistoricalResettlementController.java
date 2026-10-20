package apps.sarafrika.elimika.payout.controller;

import apps.sarafrika.elimika.payout.service.HistoricalResettlementService;
import apps.sarafrika.elimika.payout.service.HistoricalResettlementService.Report;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform-admin tool that corrects class sales settled before creators and hiring organisations were paid. */
@RestController
@RequestMapping("/api/v1/admin/payouts/historical-resettlement")
@RequiredArgsConstructor
@Tag(name = "Payout Administration", description = "One-off corrections to how past sales were settled")
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class HistoricalResettlementController {

    private final HistoricalResettlementService historicalResettlementService;

    @PostMapping
    @Operation(operationId = "resettleHistoricalClassSales", summary = "Re-settle past class sales",
            description = "Dry run unless apply=true. Pays each creator what was retained and moves a job class's "
                    + "instructor credit to the hiring organisation. Lines already re-settled are skipped.")
    public ResponseEntity<ApiResponse<Report>> resettle(@RequestParam(value = "apply", defaultValue = "false") boolean apply) {
        Report report = historicalResettlementService.run(apply);
        return ResponseEntity.ok(ApiResponse.success(report, apply ? "Re-settlement applied" : "Dry run, nothing written"));
    }
}
