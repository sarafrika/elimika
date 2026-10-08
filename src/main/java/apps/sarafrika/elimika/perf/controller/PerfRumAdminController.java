package apps.sarafrika.elimika.perf.controller;

import apps.sarafrika.elimika.perf.dto.RumSummaryResponse;
import apps.sarafrika.elimika.perf.service.PerfRumService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/admin/perf/rum")
@RequiredArgsConstructor
@Tag(name = "Performance Monitoring", description = "Real-user performance sample ingestion")
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class PerfRumAdminController {

    private final PerfRumService perfRumService;

    @GetMapping("/summary")
    @Operation(summary = "Real-user percentiles",
            description = "p50/p95/p99 per route template and metric for samples in [from, to). Defaults to the last 24 hours.")
    public ResponseEntity<ApiResponse<RumSummaryResponse>> summary(
            @Parameter(description = "Window start (ISO-8601, inclusive)")
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @Parameter(description = "Window end (ISO-8601, exclusive)")
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        try {
            return ResponseEntity.ok(ApiResponse.success(perfRumService.summarize(from, to),
                    "Performance summary retrieved"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
