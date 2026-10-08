package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.SavedAgeGroupDTO;
import apps.sarafrika.elimika.course.dto.SavedAgeGroupRequest;
import apps.sarafrika.elimika.course.internal.agegroup.SavedAgeGroupService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Saved age groups: named age bands an instructor or organisation keeps for reuse. Training
 * applications copy them, so changing one here never changes an application.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Age groups", description = "Reusable age groups kept by instructors and organisations")
public class SavedAgeGroupController {

    private static final String INSTRUCTOR = "@domainSecurityService.isInstructor()";
    private static final String ORGANISATION_MEMBER = "@domainSecurityService.belongsToOrganisation(#organisationUuid)";
    private static final String ORGANISATION_MANAGER = "@domainSecurityService.managesOrganisation(#organisationUuid)";

    private final SavedAgeGroupService service;

    @Operation(summary = "List my saved age groups", operationId = "listMyAgeGroups")
    @GetMapping("/instructors/me/age-groups")
    @PreAuthorize(INSTRUCTOR)
    public ResponseEntity<ApiResponse<List<SavedAgeGroupDTO>>> listMine() {
        return ResponseEntity.ok(ApiResponse.success(service.listMine(), "Age groups retrieved successfully"));
    }

    @Operation(summary = "Save an age group of my own", operationId = "createMyAgeGroup")
    @PostMapping("/instructors/me/age-groups")
    @PreAuthorize(INSTRUCTOR)
    public ResponseEntity<ApiResponse<SavedAgeGroupDTO>> createMine(@Valid @RequestBody SavedAgeGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.createMine(request), "Age group saved successfully"));
    }

    @Operation(summary = "Age groups to start an application from",
            description = "The caller's own saved age groups, then those of every organisation they belong to.",
            operationId = "listMyAgeGroupPresets")
    @GetMapping("/instructors/me/age-group-presets")
    @PreAuthorize(INSTRUCTOR)
    public ResponseEntity<ApiResponse<List<SavedAgeGroupDTO>>> presets() {
        return ResponseEntity.ok(ApiResponse.success(service.presetsForCurrentInstructor(),
                "Age group presets retrieved successfully"));
    }

    @Operation(summary = "List an organisation's saved age groups", operationId = "listOrganisationAgeGroups")
    @GetMapping("/organisations/{organisationUuid}/age-groups")
    @PreAuthorize(ORGANISATION_MEMBER)
    public ResponseEntity<ApiResponse<List<SavedAgeGroupDTO>>> listForOrganisation(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(service.listForOrganisation(organisationUuid),
                "Age groups retrieved successfully"));
    }

    @Operation(summary = "Save an age group for an organisation", operationId = "createOrganisationAgeGroup")
    @PostMapping("/organisations/{organisationUuid}/age-groups")
    @PreAuthorize(ORGANISATION_MANAGER)
    public ResponseEntity<ApiResponse<SavedAgeGroupDTO>> createForOrganisation(
            @PathVariable UUID organisationUuid, @Valid @RequestBody SavedAgeGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                service.createForOrganisation(organisationUuid, request), "Age group saved successfully"));
    }

    @Operation(summary = "Update a saved age group", description = "Only its owner: the instructor, or the organisation's managers.",
            operationId = "updateAgeGroup")
    @PutMapping("/age-groups/{ageGroupUuid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SavedAgeGroupDTO>> update(@PathVariable UUID ageGroupUuid,
                                                                @Valid @RequestBody SavedAgeGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(ageGroupUuid, request), "Age group updated successfully"));
    }

    @Operation(summary = "Delete a saved age group", description = "Only its owner. Applications that copied it keep their copy.",
            operationId = "deleteAgeGroup")
    @DeleteMapping("/age-groups/{ageGroupUuid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID ageGroupUuid) {
        service.delete(ageGroupUuid);
        return ResponseEntity.noContent().build();
    }
}
