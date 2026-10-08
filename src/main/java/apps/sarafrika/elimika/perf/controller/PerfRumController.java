package apps.sarafrika.elimika.perf.controller;

import apps.sarafrika.elimika.perf.dto.RumBatchRequest;
import apps.sarafrika.elimika.perf.dto.RumIngestResponse;
import apps.sarafrika.elimika.perf.internal.RumRateLimiter;
import apps.sarafrika.elimika.perf.service.PerfRumService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Beacon target for the web app's real-user timings; open to anonymous visitors under a tighter limit. */
@RestController
@RequestMapping("/api/v1/perf/rum")
@RequiredArgsConstructor
@Tag(name = "Performance Monitoring", description = "Real-user performance sample ingestion")
public class PerfRumController {

    private final PerfRumService perfRumService;
    private final RumRateLimiter rateLimiter;

    @PostMapping
    @Operation(summary = "Ingest real-user performance samples",
            description = "Accepts up to 50 samples per batch. Anonymous callers are rate limited per IP.")
    public ResponseEntity<ApiResponse<RumIngestResponse>> ingest(@Valid @RequestBody RumBatchRequest request,
                                                                 HttpServletRequest httpRequest) {
        String subject = currentSubject();
        if (!rateLimiter.tryAcquire(subject, httpRequest)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many samples. Please slow down.");
        }
        RumIngestResponse response = perfRumService.ingest(request.events(), subject != null);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Performance samples recorded"));
    }

    private static String currentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
