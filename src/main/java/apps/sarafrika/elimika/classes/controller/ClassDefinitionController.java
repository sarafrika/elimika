package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.RecurrenceEngineService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Definition Management", description = "APIs for creating and managing class definitions, recurrence patterns, and scheduling.")

public class ClassDefinitionController {

    private final ClassDefinitionServiceInterface classDefinitionService;
    private final RecurrenceEngineService recurrenceEngineService;
    private final TimetableService timetableService;

    // ================================
    // CORE CLASS DEFINITION MANAGEMENT
    // ================================

    @Operation(summary = "Create a new class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Class definition created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<ClassDefinitionDTO>> createClassDefinition(
            @Valid @RequestBody ClassDefinitionDTO request) {
        log.debug("REST request to create class definition: {}", request.title());
        
        ClassDefinitionDTO result = classDefinitionService.createClassDefinition(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Class definition created successfully"));
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

    // ================================
    // RECURRENCE PATTERN MANAGEMENT
    // ================================

    @Operation(summary = "Create a new recurrence pattern")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Recurrence pattern created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping("/recurrence-patterns")
    public ResponseEntity<ApiResponse<RecurrencePatternDTO>> createClassRecurrencePattern(
            @Valid @RequestBody RecurrencePatternDTO request) {
        log.debug("REST request to create recurrence pattern: {}", request.recurrenceType());
        
        RecurrencePatternDTO result = classDefinitionService.createRecurrencePattern(request);
        return ResponseEntity.status(201).body(ApiResponse.success(result, "Recurrence pattern created successfully"));
    }

    @Operation(summary = "Get a recurrence pattern by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recurrence pattern retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Recurrence pattern not found")
    @GetMapping("/recurrence-patterns/{uuid}")
    public ResponseEntity<ApiResponse<RecurrencePatternDTO>> getClassRecurrencePattern(
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
    public ResponseEntity<ApiResponse<RecurrencePatternDTO>> updateClassRecurrencePattern(
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
    public ResponseEntity<ApiResponse<Void>> deleteClassRecurrencePattern(
            @Parameter(description = "UUID of the recurrence pattern to delete", required = true)
            @PathVariable UUID uuid) {
        log.debug("REST request to delete recurrence pattern: {}", uuid);
        
        classDefinitionService.deleteRecurrencePattern(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Recurrence pattern deleted successfully"));
    }

    // ================================
    // RECURRING SCHEDULE MANAGEMENT (Google Calendar-like functionality)
    // ================================

    @Operation(summary = "Schedule recurring classes from a class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Recurring schedule created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data or class definition has no recurrence pattern")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @PostMapping("/{uuid}/schedule")
    public ResponseEntity<ApiResponse<List<ScheduledInstanceDTO>>> scheduleRecurringClassFromDefinition(
            @Parameter(description = "UUID of the class definition to schedule", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Date to start scheduling from (YYYY-MM-DD)", required = true)
            @RequestParam LocalDate startDate,
            @Parameter(description = "Date to stop scheduling (optional, uses pattern end date if not provided)")
            @RequestParam(required = false) LocalDate endDate) {
        
        log.debug("REST request to schedule recurring class: {} from {} to {}", uuid, startDate, endDate);
        
        List<ScheduledInstanceDTO> result = recurrenceEngineService.scheduleRecurringClass(uuid, startDate, endDate);
        return ResponseEntity.status(201).body(ApiResponse.success(result, 
            String.format("Created %d recurring class instances", result.size())));
    }

    @Operation(summary = "Update recurring schedule for a class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recurring schedule updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @PutMapping("/{uuid}/schedule")
    public ResponseEntity<ApiResponse<List<ScheduledInstanceDTO>>> updateRecurringClassSchedule(
            @Parameter(description = "UUID of the class definition to update schedule for", required = true)
            @PathVariable UUID uuid) {
        
        log.debug("REST request to update recurring schedule for class: {}", uuid);
        
        List<ScheduledInstanceDTO> result = recurrenceEngineService.updateRecurringSchedule(uuid);
        return ResponseEntity.ok(ApiResponse.success(result, 
            String.format("Updated recurring schedule with %d instances", result.size())));
    }

    @Operation(summary = "Cancel recurring schedule for a class definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recurring schedule cancelled successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @DeleteMapping("/{uuid}/schedule")
    public ResponseEntity<ApiResponse<Void>> cancelRecurringClassSchedule(
            @Parameter(description = "UUID of the class definition to cancel schedule for", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Reason for cancellation", required = true)
            @RequestParam String reason) {
        
        log.debug("REST request to cancel recurring schedule for class: {} with reason: {}", uuid, reason);
        
        int cancelledCount = recurrenceEngineService.cancelRecurringSchedule(uuid, reason);
        return ResponseEntity.ok(ApiResponse.success(null, 
            String.format("Cancelled %d future class instances", cancelledCount)));
    }

    @Operation(summary = "Preview recurring schedule without creating instances")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Schedule preview generated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data or class definition has no recurrence pattern")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Class definition not found")
    @GetMapping("/{uuid}/schedule/preview")
    public ResponseEntity<ApiResponse<List<ScheduledInstanceDTO>>> previewRecurringClassSchedule(
            @Parameter(description = "UUID of the class definition to preview", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Date to start preview from (YYYY-MM-DD)", required = true)
            @RequestParam LocalDate startDate,
            @Parameter(description = "Date to stop preview (optional, uses pattern end date if not provided)")
            @RequestParam(required = false) LocalDate endDate) {
        
        log.debug("REST request to preview recurring schedule for class: {} from {} to {}", uuid, startDate, endDate);
        
        ClassDefinitionDTO classDefinition = classDefinitionService.getClassDefinition(uuid);
        List<ScheduledInstanceDTO> result = recurrenceEngineService.generateScheduledInstances(
                classDefinition, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(result, 
            String.format("Preview shows %d potential class instances", result.size())));
    }

    @Operation(summary = "Check for scheduling conflicts")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conflict check completed")
    @GetMapping("/{uuid}/schedule/conflicts")
    public ResponseEntity<ApiResponse<List<ScheduledInstanceDTO>>> checkClassSchedulingConflicts(
            @Parameter(description = "UUID of the class definition to check", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Date to start checking from (YYYY-MM-DD)", required = true)
            @RequestParam LocalDate startDate,
            @Parameter(description = "Date to stop checking (optional)")
            @RequestParam(required = false) LocalDate endDate) {
        
        log.debug("REST request to check scheduling conflicts for class: {} from {} to {}", uuid, startDate, endDate);
        
        ClassDefinitionDTO classDefinition = classDefinitionService.getClassDefinition(uuid);
        List<ScheduledInstanceDTO> potentialInstances = recurrenceEngineService.generateScheduledInstances(
                classDefinition, startDate, endDate);
        
        List<ScheduledInstanceDTO> conflicts = recurrenceEngineService.checkSchedulingConflicts(potentialInstances);
        
        return ResponseEntity.ok(ApiResponse.success(conflicts, 
            String.format("Found %d scheduling conflicts out of %d potential instances", 
                conflicts.size(), potentialInstances.size())));
    }
}
