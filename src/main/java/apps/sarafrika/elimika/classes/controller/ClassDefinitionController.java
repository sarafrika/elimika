package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionCreationResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Definition Management", description = "APIs for creating and managing class definitions and scheduling.")

public class ClassDefinitionController {

    private final ClassDefinitionServiceInterface classDefinitionService;
    private final TimetableService timetableService;

    // ================================
    // CORE CLASS DEFINITION MANAGEMENT
    // ================================

    @Operation(summary = "Create a new class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class definition created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<ClassDefinitionCreationResponseDTO>> createClassDefinition(
            @Valid @RequestBody ClassDefinitionDTO request) {
        log.debug("REST request to create class definition: {}", request.title());

        try {
            ClassDefinitionCreationResponseDTO result = classDefinitionService.createClassDefinition(request);
            return ResponseEntity.status(201).body(ApiResponse.success(result, "Class definition created successfully"));
        } catch (SchedulingConflictException e) {
            log.warn("Scheduling conflicts while creating class definition {}: {}", request.title(), e.getMessage());
            return ResponseEntity.status(409).body(ApiResponse.error("Scheduling conflicts detected", e.getConflicts()));
        }
    }

    @Operation(summary = "List enrollments for a class definition across all scheduled instances")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully")
    @GetMapping("/{uuid}/enrollments")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getEnrollmentsForClass(
            @Parameter(description = "UUID of the class definition", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to get enrollments for class definition: {}", uuid);

        List<EnrollmentDTO> enrollments = timetableService.getEnrollmentsForClass(uuid);
        return ResponseEntity.ok(ApiResponse.success(enrollments, "Enrollments retrieved successfully"));
    }

    @Operation(summary = "Get a class definition by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class definition retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ClassDefinitionDTO>> getClassDefinition(
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
    public ResponseEntity<ApiResponse<ClassDefinitionDTO>> updateClassDefinition(
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
    public ResponseEntity<ApiResponse<Void>> deactivateClassDefinition(
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
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getClassDefinitionsForCourse(
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
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getClassDefinitionsForInstructor(
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
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getClassDefinitionsForOrganisation(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid) {
        log.debug("REST request to get classes for organisation: {}", organisationUuid);
        
        List<ClassDefinitionDTO> result = classDefinitionService.findClassesForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(result, "Class definitions for organisation retrieved successfully"));
    }

    @Operation(summary = "Get all active class definitions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active class definitions retrieved successfully")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ClassDefinitionDTO>>> getAllActiveClassDefinitions() {
        log.debug("REST request to get all active classes");
        
        List<ClassDefinitionDTO> result = classDefinitionService.findAllActiveClasses();
        return ResponseEntity.ok(ApiResponse.success(result, "All active class definitions retrieved successfully"));
    }

}
