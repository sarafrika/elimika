package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.RegistrationAcceptedDTO;
import apps.sarafrika.elimika.tenancy.dto.RegistrationRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.RegistrationResendRequestDTO;
import apps.sarafrika.elimika.tenancy.internal.RegistrationRateLimiter;
import apps.sarafrika.elimika.tenancy.services.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(RegistrationController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Registration", description = "Self-registration into Elimika and domain applications")
public class RegistrationController {

    public static final String API_ROOT_PATH = "/api/v1/registrations";

    private static final RegistrationAcceptedDTO ACCEPTED = new RegistrationAcceptedDTO(
            "If the address can be registered, a link to set your password is on its way. "
                    + "Already have a Sarafrika account? Sign in instead.");

    private final RegistrationService registrationService;
    private final RegistrationRateLimiter rateLimiter;
    private final DomainSecurityService domainSecurityService;

    @Operation(operationId = "register", summary = "Register a new account",
            description = "Creates the Keycloak account, which emails a set-password link, and records the chosen "
                    + "domain as pending approval. Answers the same way when the email is already registered.")
    @PostMapping
    public ResponseEntity<ApiResponse<RegistrationAcceptedDTO>> register(
            @Valid @RequestBody RegistrationRequestDTO request, HttpServletRequest httpRequest) {
        enforceRateLimit(httpRequest, request.email());
        registrationService.register(request, RegistrationRateLimiter.clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(ACCEPTED, "Registration received"));
    }

    @Operation(operationId = "resendRegistrationEmail", summary = "Resend the set-password email",
            description = "Sends the email again while the account still has its password or email verification "
                    + "outstanding. Answers the same way whether or not anything was sent.")
    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<RegistrationAcceptedDTO>> resend(
            @Valid @RequestBody RegistrationResendRequestDTO request, HttpServletRequest httpRequest) {
        enforceRateLimit(httpRequest, request.email());
        registrationService.resendActionsEmail(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(ACCEPTED, "Request received"));
    }

    @Operation(operationId = "applyForDomain", summary = "Apply for another domain",
            description = "For a signed-in account, such as an existing Sarafrika user joining Elimika. The domain "
                    + "stays pending until a platform admin approves it.")
    @PostMapping("/me/domains")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DomainApplicationDTO>> applyForDomain(
            @Valid @RequestBody DomainApplicationRequestDTO request) {
        UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
        if (currentUserUuid == null) {
            throw new ResourceNotFoundException("No user record for the authenticated caller");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                registrationService.applyForDomain(currentUserUuid, request.domain()), "Domain application recorded"));
    }

    private void enforceRateLimit(HttpServletRequest httpRequest, String email) {
        if (!rateLimiter.tryAcquire(httpRequest, email)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please try again later.");
        }
    }
}
