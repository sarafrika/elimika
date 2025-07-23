package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.services.TrainingBranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/training-branches")
@RequiredArgsConstructor
@Tag(name = "Training Branches API", description = "Training branches related operations")
class TrainingBranchController {
    private final TrainingBranchService trainingBranchService;

    @Operation(summary = "Create a new training branch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Training branch created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> createTrainingBranch(
            @Valid @RequestBody TrainingBranchDTO trainingBranchDTO) {
        TrainingBranchDTO created = trainingBranchService.createTrainingBranch(trainingBranchDTO);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Training branch created successfully"));
    }

    @Operation(summary = "Get a training branch by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> getTrainingBranchByUuid(@PathVariable UUID uuid) {
        TrainingBranchDTO trainingBranch = trainingBranchService.getTrainingBranchByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(trainingBranch, "Training branch retrieved successfully"));
    }

    @Operation(summary = "Get all training branches")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branches retrieved successfully")
    @GetMapping
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
    public ResponseEntity<ApiResponse<TrainingBranchDTO>> updateTrainingBranch(
            @PathVariable UUID uuid, @Valid @RequestBody TrainingBranchDTO trainingBranchDTO) {
        TrainingBranchDTO updated = trainingBranchService.updateTrainingBranch(uuid, trainingBranchDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Training branch updated successfully"));
    }

    @Operation(summary = "Delete a training branch by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Training branch deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Training branch not found")
    @DeleteMapping("/{uuid}")
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
    public ResponseEntity<ApiResponse<PagedDTO<TrainingBranchDTO>>> search(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object"))
            @RequestParam() Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(trainingBranchService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Training branches search successful"));
    }
}