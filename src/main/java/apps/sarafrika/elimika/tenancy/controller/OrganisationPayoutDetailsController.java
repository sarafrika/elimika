package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.OrganisationPayoutDetailsDTO;
import apps.sarafrika.elimika.tenancy.service.OrganisationPayoutDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/organisations/{organisationUuid}/payout-details")
@RequiredArgsConstructor
@Tag(name = "Organisation Payout Details API", description = "The M-Pesa number an organisation wants to be paid on")
public class OrganisationPayoutDetailsController {

    // Payout details are for the organisation's managers (and platform admins), not every member.
    private static final String MANAGE_ORGANISATION =
            "@organisationSecurityService.canManageOrganisation(#organisationUuid)";

    private final OrganisationPayoutDetailsService payoutDetailsService;

    @Operation(operationId = "getOrganisationPayoutDetails", summary = "Get organisation payout details",
            description = "404 until details are saved.")
    @GetMapping
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<OrganisationPayoutDetailsDTO>> get(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(payoutDetailsService.get(organisationUuid),
                "Payout details retrieved successfully"));
    }

    @Operation(operationId = "saveOrganisationPayoutDetails", summary = "Save organisation payout details",
            description = "Creates or replaces the organisation's M-Pesa number (Kenya). No code is sent; the "
                    + "platform admin who verifies the organisation checks it. Completes the organisation_payout "
                    + "onboarding step.")
    @PutMapping
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<OrganisationPayoutDetailsDTO>> save(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody OrganisationPayoutDetailsDTO details) {
        return ResponseEntity.ok(ApiResponse.success(payoutDetailsService.save(organisationUuid, details),
                "Payout details saved successfully"));
    }
}
