package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.services.OrganisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/organisations")
@RequiredArgsConstructor @Tag(name = "Organisations API", description = "Organisations related operations")
class OrganisationController {
    private final OrganisationService organisationService;

    @Operation(summary = "Create a new organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Organisation created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<OrganisationDTO>> createOrganisation(
            @Valid @RequestBody OrganisationDTO organisationDTO) {
        OrganisationDTO created = organisationService.createOrganisation(organisationDTO);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Organisation created successfully"));
    }

    @Operation(summary = "Get an organisation by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisation retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @GetMapping("/{uuid}") @PreAuthorize("hasAuthority('organisation:read')")
    public ResponseEntity<ApiResponse<OrganisationDTO>> getOrganisationByUuid(@PathVariable UUID uuid) {
        OrganisationDTO organisation = organisationService.getOrganisationByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(organisation, "Organisation retrieved successfully"));
    }

    @Operation(summary = "Get all organisations")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisations retrieved successfully")
    @GetMapping @PreAuthorize("hasAuthority('organisation:read_all')")
    public ResponseEntity<ApiResponse<PagedDTO<OrganisationDTO>>> getAllOrganisations(Pageable pageable) {
        Page<OrganisationDTO> organisations = organisationService.getAllOrganisations(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(organisations, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Organisations retrieved successfully"));
    }

    @Operation(summary = "Update an organisation by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisation updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/{uuid}") @PreAuthorize("hasAuthority('organisation:update')")
    public ResponseEntity<ApiResponse<OrganisationDTO>> updateOrganisation(
            @PathVariable UUID uuid, @Valid @RequestBody OrganisationDTO organisationDTO) {
        OrganisationDTO updated = organisationService.updateOrganisation(uuid, organisationDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Organisation updated successfully"));
    }

    @Operation(summary = "Delete an organisation by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisation deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @DeleteMapping("/{uuid}") @PreAuthorize("hasAuthority('organisation:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteOrganisation(@PathVariable UUID uuid) {
        organisationService.deleteOrganisation(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Organisation deleted successfully"));
    }

    @Operation(summary = "Search organisations",
            description = "Fetches a paginated list of organisations based on optional filters. " +
                    "Supports pagination and sorting.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of organisations matching the search criteria")
    @GetMapping("search") @PreAuthorize("hasAuthority('organisation:read_all')")
    public ResponseEntity<ApiResponse<PagedDTO<OrganisationDTO>>> search(
            @RequestParam(required = false) Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(organisationService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Organisations search successful"));
    }
}
