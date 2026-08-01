package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.services.TrainingBranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("api/v1/training-branches")
@RequiredArgsConstructor
@Tag(name = "Training Branches API", description = "Training branches related operations")
class TrainingBranchController {

    /**
     * A branch belongs to exactly one organisation, so every branch-scoped route is authorised
     * against that organisation. Creation is the exception: there is no branch yet, so the target
     * organisation is taken from the request body.
     */
    private static final String CREATE_IN_BODY_ORGANISATION =
            "@organisationSecurityService.canManageOrganisation(#trainingBranchDTO.organisationUuid())";
    private static final String READ_BRANCH = "@organisationSecurityService.canReadBranch(#uuid)";
    private static final String MANAGE_BRANCH = "@organisationSecurityService.canManageBranch(#uuid)";
    private static final String READ_ORGANISATION =
            "@organisationSecurityService.canReadOrganisation(#organisationUuid)";
    /**
     * Cross-tenant enumerations: they take no organisation filter, so any answer would leak every
     * tenant's branches. Restricted to platform admins rather than narrowed, because no caller in
     * the product needs an unscoped listing.
     */
    private static final String PLATFORM_ADMIN = "@domainSecurityService.isPlatformAdmin()";

    private final TrainingBranchService trainingBranchService;

    @Operation(summary = "Create a new training branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Training branch created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    @PreAuthorize(CREATE_IN_BODY_ORGANISATION)
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> createTrainingBranch(
            @Valid @RequestBody TrainingBranchDTO trainingBranchDTO) {
        TrainingBranchDTO created = trainingBranchService.createTrainingBranch(trainingBranchDTO);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Training branch created successfully"));
    }

    @Operation(summary = "Get a training branch by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @GetMapping("/{uuid}")
    @PreAuthorize(READ_BRANCH)
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> getTrainingBranchByUuid(@PathVariable UUID uuid) {
        TrainingBranchDTO trainingBranch = trainingBranchService.getTrainingBranchByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(trainingBranch, "Training branch retrieved successfully"));
    }

    @Operation(summary = "Get all training branches")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branches retrieved successfully")
    @GetMapping
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<ApiResponse<PagedDTO<TrainingBranchDTO>>> getAllTrainingBranches(Pageable pageable) {
        Page<TrainingBranchDTO> trainingBranches = trainingBranchService.getAllTrainingBranches(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(trainingBranches, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Training branches retrieved successfully"));
    }

    @Operation(summary = "Get training branches by organisation UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branches retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organisation not found")
    @GetMapping("/organisation/{organisationUuid}")
    @PreAuthorize(READ_ORGANISATION)
    public ResponseEntity<ApiResponse<PagedDTO<TrainingBranchDTO>>> getTrainingBranchesByOrganisation(
            @PathVariable UUID organisationUuid, Pageable pageable) {
        Page<TrainingBranchDTO> trainingBranches = trainingBranchService.getTrainingBranchesByOrganisation(organisationUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(trainingBranches, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Training branches retrieved successfully"));
    }

    @Operation(summary = "Update a training branch by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/{uuid}")
    @PreAuthorize(MANAGE_BRANCH)
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> updateTrainingBranch(
            @PathVariable UUID uuid, @Valid @RequestBody TrainingBranchDTO trainingBranchDTO) {
        TrainingBranchDTO updated = trainingBranchService.updateTrainingBranch(uuid, trainingBranchDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Training branch updated successfully"));
    }

    @Operation(summary = "Delete a training branch by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @DeleteMapping("/{uuid}")
    @PreAuthorize(MANAGE_BRANCH)
    public ResponseEntity<ApiResponse<Void>> deleteTrainingBranch(@PathVariable UUID uuid) {
        trainingBranchService.deleteTrainingBranch(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Training branch deleted successfully"));
    }

    @Operation(summary = "Search training branches",
            description = "Fetches a paginated list of training branches based on optional filters. " +
                    "Supports pagination and sorting.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of training branches matching the search criteria")
    @GetMapping("search")
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<ApiResponse<PagedDTO<TrainingBranchDTO>>> search(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam() Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(trainingBranchService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Training branches search successful"));
    }
}