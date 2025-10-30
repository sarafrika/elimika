package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassAssignmentScheduleDTO;
import apps.sarafrika.elimika.classes.dto.ClassLessonPlanDTO;
import apps.sarafrika.elimika.classes.dto.ClassQuizScheduleDTO;
import apps.sarafrika.elimika.classes.service.ClassAssessmentScheduleService;
import apps.sarafrika.elimika.classes.service.ClassLessonPlanService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ClassScheduleController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Class Scheduling Management", description = "Manage lesson plans and assessment schedules for instructor-led classes.")
public class ClassScheduleController {

    public static final String API_ROOT_PATH = "/api/v1/classes/{classUuid}";

    private final ClassLessonPlanService classLessonPlanService;
    private final ClassAssessmentScheduleService classAssessmentScheduleService;

    // Lesson plan endpoints
    @Operation(summary = "Get the lesson plan for a class definition")
    @GetMapping("/lesson-plan")
    public ResponseEntity<ApiResponse<List<ClassLessonPlanDTO>>> getLessonPlan(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid) {
        log.debug("Fetching lesson plan for class {}", classUuid);
        List<ClassLessonPlanDTO> lessonPlan = classLessonPlanService.getLessonPlan(classUuid);
        return ResponseEntity.ok(ApiResponse.success(lessonPlan, "Lesson plan retrieved successfully"));
    }

    @Operation(summary = "Replace the lesson plan for a class definition")
    @PutMapping("/lesson-plan")
    public ResponseEntity<ApiResponse<List<ClassLessonPlanDTO>>> saveLessonPlan(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid,
            @RequestBody List<ClassLessonPlanDTO> request) {
        log.debug("Updating lesson plan for class {} with {} entries", classUuid, request != null ? request.size() : 0);
        List<ClassLessonPlanDTO> result = classLessonPlanService.saveLessonPlan(classUuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Lesson plan saved successfully"));
    }

    // Assignment schedules
    @Operation(summary = "List assignment schedules for a class definition")
    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<ClassAssignmentScheduleDTO>>> getAssignmentSchedules(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid) {
        log.debug("Fetching assignment schedules for class {}", classUuid);
        List<ClassAssignmentScheduleDTO> schedules = classAssessmentScheduleService.getAssignmentSchedules(classUuid);
        return ResponseEntity.ok(ApiResponse.success(schedules, "Assignment schedules retrieved successfully"));
    }

    @Operation(summary = "Create an assignment schedule for a class definition")
    @PostMapping("/assignments")
    public ResponseEntity<ApiResponse<ClassAssignmentScheduleDTO>> createAssignmentSchedule(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid,
            @RequestBody ClassAssignmentScheduleDTO request) {
        log.debug("Creating assignment schedule for class {}", classUuid);
        ClassAssignmentScheduleDTO result = classAssessmentScheduleService.createAssignmentSchedule(classUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Assignment schedule created successfully"));
    }

    @Operation(summary = "Update an assignment schedule for a class definition")
    @PatchMapping("/assignments/{scheduleUuid}")
    public ResponseEntity<ApiResponse<ClassAssignmentScheduleDTO>> updateAssignmentSchedule(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid,
            @Parameter(description = "Assignment schedule UUID", required = true)
            @PathVariable UUID scheduleUuid,
            @RequestBody ClassAssignmentScheduleDTO request) {
        log.debug("Updating assignment schedule {} for class {}", scheduleUuid, classUuid);
        ClassAssignmentScheduleDTO result = classAssessmentScheduleService.updateAssignmentSchedule(classUuid, scheduleUuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Assignment schedule updated successfully"));
    }

    @Operation(summary = "Delete an assignment schedule for a class definition")
    @DeleteMapping("/assignments/{scheduleUuid}")
    public ResponseEntity<Void> deleteAssignmentSchedule(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid,
            @Parameter(description = "Assignment schedule UUID", required = true)
            @PathVariable UUID scheduleUuid) {
        log.debug("Deleting assignment schedule {} for class {}", scheduleUuid, classUuid);
        classAssessmentScheduleService.deleteAssignmentSchedule(classUuid, scheduleUuid);
        return ResponseEntity.noContent().build();
    }

    // Quiz schedules
    @Operation(summary = "List quiz schedules for a class definition")
    @GetMapping("/quizzes")
    public ResponseEntity<ApiResponse<List<ClassQuizScheduleDTO>>> getQuizSchedules(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid) {
        log.debug("Fetching quiz schedules for class {}", classUuid);
        List<ClassQuizScheduleDTO> schedules = classAssessmentScheduleService.getQuizSchedules(classUuid);
        return ResponseEntity.ok(ApiResponse.success(schedules, "Quiz schedules retrieved successfully"));
    }

    @Operation(summary = "Create a quiz schedule for a class definition")
    @PostMapping("/quizzes")
    public ResponseEntity<ApiResponse<ClassQuizScheduleDTO>> createQuizSchedule(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid,
            @RequestBody ClassQuizScheduleDTO request) {
        log.debug("Creating quiz schedule for class {}", classUuid);
        ClassQuizScheduleDTO result = classAssessmentScheduleService.createQuizSchedule(classUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Quiz schedule created successfully"));
    }

    @Operation(summary = "Update a quiz schedule for a class definition")
    @PatchMapping("/quizzes/{scheduleUuid}")
    public ResponseEntity<ApiResponse<ClassQuizScheduleDTO>> updateQuizSchedule(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid,
            @Parameter(description = "Quiz schedule UUID", required = true)
            @PathVariable UUID scheduleUuid,
            @RequestBody ClassQuizScheduleDTO request) {
        log.debug("Updating quiz schedule {} for class {}", scheduleUuid, classUuid);
        ClassQuizScheduleDTO result = classAssessmentScheduleService.updateQuizSchedule(classUuid, scheduleUuid, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Quiz schedule updated successfully"));
    }

    @Operation(summary = "Delete a quiz schedule for a class definition")
    @DeleteMapping("/quizzes/{scheduleUuid}")
    public ResponseEntity<Void> deleteQuizSchedule(
            @Parameter(description = "Class definition UUID", required = true)
            @PathVariable UUID classUuid,
            @Parameter(description = "Quiz schedule UUID", required = true)
            @PathVariable UUID scheduleUuid) {
        log.debug("Deleting quiz schedule {} for class {}", scheduleUuid, classUuid);
        classAssessmentScheduleService.deleteQuizSchedule(classUuid, scheduleUuid);
        return ResponseEntity.noContent().build();
    }
}
