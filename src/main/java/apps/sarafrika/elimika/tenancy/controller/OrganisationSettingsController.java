package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.OrganisationSettingsDTO;
import apps.sarafrika.elimika.tenancy.service.OrganisationSettingsService;
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
@RequestMapping("api/v1/organisations/{organisationUuid}/settings")
@RequiredArgsConstructor
@Tag(name = "Organisation Settings API", description = "Settings an organisation chooses for itself")
public class OrganisationSettingsController {

    private static final String READ_ORGANISATION =
            "@organisationSecurityService.canReadOrganisation(#organisationUuid)";
    private static final String MANAGE_ORGANISATION =
            "@organisationSecurityService.canManageOrganisation(#organisationUuid)";

    private final OrganisationSettingsService settingsService;

    @Operation(summary = "Get organisation settings", description = "Includes how long submitted session reports stay editable.")
    @GetMapping
    @PreAuthorize(READ_ORGANISATION)
    public ResponseEntity<ApiResponse<OrganisationSettingsDTO>> get(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.get(organisationUuid), "Settings retrieved successfully"));
    }

    @Operation(summary = "Update organisation settings", description = "Only the organisation's managers may change its settings.")
    @PutMapping
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<OrganisationSettingsDTO>> update(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody OrganisationSettingsDTO settings) {
        return ResponseEntity.ok(ApiResponse.success(settingsService.update(organisationUuid, settings), "Settings saved successfully"));
    }
}
