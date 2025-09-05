package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Definitions API", description = "Complete class definition management including templates, recurrence patterns, and scheduling rules")
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
public class ClassDefinitionController {

    private final ClassDefinitionServiceInterface classDefinitionService;

    // ================================
    // CORE CLASS DEFINITION MANAGEMENT
    // ================================

    @Operation(summary = "Create a new class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class definition created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<ClassDefinitionDTO>> createDefinition(
            @Valid @RequestBody ClassDefinitionDTO request) {
        log.debug("REST request to create class definition: {}", request.title());
        
        ClassDefinitionDTO result = classDefinitionService.createClassDefinition(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Class definition created successfully"));
    }

    @Operation(summary = "Get a class definition by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definition retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ClassDefinitionDTO>> getDefinition(
            @Parameter(description = "UUID of the class definition to retrieve", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to get class definition: {}", uuid);
        
        ClassDefinitionDTO result = classDefinitionService.getClassDefinition(uuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definition retrieved successfully"));
    }

    @Operation(summary = "Update a class definition by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definition updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ClassDefinitionDTO>> updateDefinition(
            @Parameter(description = "UUID of the class definition to update", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody ClassDefinitionDTO request) {
        log.debug("REST request to update class definition: {}", uuid);
        
        ClassDefinitionDTO result = classDefinitionService.updateClassDefinition(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definition updated successfully"));
    }

    @Operation(summary = "Deactivate a class definition by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definition deactivated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deactivateDefinition(
            @Parameter(description = "UUID of the class definition to deactivate", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to deactivate class definition: {}", uuid);
        
        classDefinitionService.deactivateClassDefinition(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Class definition deactivated successfully"));
    }

    // ================================
    // CLASS DEFINITION QUERIES
    // ================================

    @Operation(summary = "Get class definitions for a course")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping("/course/{courseUuid}")
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getClassesForCourse(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Whether to include only active class definitions")
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        log.debug("REST request to get classes for course: {} (activeOnly: {})", courseUuid, activeOnly);
        
        List<ClassDefinitionDTO> result = activeOnly 
            ? classDefinitionService.findActiveClassesForCourse(courseUuid)
            : classDefinitionService.findClassesForCourse(courseUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for course retrieved successfully"));
    }

    @Operation(summary = "Get class definitions for an instructor")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping("/instructor/{instructorUuid}")
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getClassesForInstructor(
            @Parameter(description = "UUID of the instructor", required = true)
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Whether to include only active class definitions")
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        log.debug("REST request to get classes for instructor: {} (activeOnly: {})", instructorUuid, activeOnly);
        
        List<ClassDefinitionDTO> result = activeOnly 
            ? classDefinitionService.findActiveClassesForInstructor(instructorUuid)
            : classDefinitionService.findClassesForInstructor(instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for instructor retrieved successfully"));
    }

    @Operation(summary = "Get class definitions for an organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definitions retrieved successfully")
    @GetMapping("/organisation/{organisationUuid}")
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getClassesForOrganisation(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid) {
        log.debug("REST request to get classes for organisation: {}", organisationUuid);
        
        List<ClassDefinitionDTO> result = classDefinitionService.findClassesForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for organisation retrieved successfully"));
    }

    @Operation(summary = "Get all active class definitions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active class definitions retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getAllActiveClasses() {
        log.debug("REST request to get all active classes");
        
        List<ClassDefinitionDTO> result = classDefinitionService.findAllActiveClasses();
        return ResponseEntity.ok(ApiResponse.success(result, "All active class definitions retrieved successfully"));
    }

    // ================================
    // RECURRENCE PATTERN MANAGEMENT
    // ================================

    @Operation(summary = "Create a new recurrence pattern")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Recurrence pattern created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/recurrence-patterns")
    public ResponseEntity<ApiResponse<RecurrencePatternDTO>> createRecurrencePattern(
            @Valid @RequestBody RecurrencePatternDTO request) {
        log.debug("REST request to create recurrence pattern: {}", request.recurrenceType());
        
        RecurrencePatternDTO result = classDefinitionService.createRecurrencePattern(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Recurrence pattern created successfully"));
    }

    @Operation(summary = "Get a recurrence pattern by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recurrence pattern retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recurrence pattern not found")
    @GetMapping("/recurrence-patterns/{uuid}")
    public ResponseEntity<ApiResponse<RecurrencePatternDTO>> getRecurrencePattern(
            @Parameter(description = "UUID of the recurrence pattern to retrieve", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to get recurrence pattern: {}", uuid);
        
        RecurrencePatternDTO result = classDefinitionService.getRecurrencePattern(uuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Recurrence pattern retrieved successfully"));
    }

    @Operation(summary = "Update a recurrence pattern by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recurrence pattern updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recurrence pattern not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/recurrence-patterns/{uuid}")
    public ResponseEntity<ApiResponse<RecurrencePatternDTO>> updateRecurrencePattern(
            @Parameter(description = "UUID of the recurrence pattern to update", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody RecurrencePatternDTO request) {
        log.debug("REST request to update recurrence pattern: {}", uuid);
        
        RecurrencePatternDTO result = classDefinitionService.updateRecurrencePattern(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Recurrence pattern updated successfully"));
    }

    @Operation(summary = "Delete a recurrence pattern by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recurrence pattern deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Recurrence pattern is still in use")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recurrence pattern not found")
    @DeleteMapping("/recurrence-patterns/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteRecurrencePattern(
            @Parameter(description = "UUID of the recurrence pattern to delete", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to delete recurrence pattern: {}", uuid);
        
        classDefinitionService.deleteRecurrencePattern(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Recurrence pattern deleted successfully"));
    }
}