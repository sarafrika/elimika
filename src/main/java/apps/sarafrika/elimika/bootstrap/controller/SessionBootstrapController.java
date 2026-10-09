package apps.sarafrika.elimika.bootstrap.controller;

import apps.sarafrika.elimika.bootstrap.dto.SessionBootstrapDTO;
import apps.sarafrika.elimika.bootstrap.service.SessionBootstrapService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Self-scoped like {@code /users/me}: identity comes from the token, so there is nothing to enumerate. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Session Bootstrap", description = "One-call session bootstrap for the dashboard shell")
class SessionBootstrapController {

    private final SessionBootstrapService sessionBootstrapService;
    private final DomainSecurityService domainSecurityService;

    @Operation(operationId = "getSessionBootstrap", summary = "Get the caller's session bootstrap",
            description = "Returns the caller's user record (as GET /users/me), role profile UUIDs, active "
                    + "organisation summary, wallet balance and unread notification counts per domain.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bootstrap retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Authenticated caller has no user record")
    @GetMapping("/bootstrap")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SessionBootstrapDTO>> getBootstrap() {
        UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
        if (currentUserUuid == null) {
            throw new ResourceNotFoundException("No user record for the authenticated caller");
        }
        return ResponseEntity.ok(ApiResponse.success(sessionBootstrapService.bootstrap(currentUserUuid),
                "Session bootstrap retrieved successfully"));
    }
}
