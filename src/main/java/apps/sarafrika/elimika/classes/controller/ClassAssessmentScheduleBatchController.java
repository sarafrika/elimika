package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.dto.ClassAssessmentSchedulesDTO;
import apps.sarafrika.elimika.classes.service.ClassAssessmentScheduleService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Reads the assessment calendars of many classes in one request, so a dashboard showing several
 * enrolled or taught classes does not issue one assignments and one quizzes call per class.
 */
@RestController
@RequestMapping(ClassAssessmentScheduleBatchController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Class Scheduling Management", description = "Manage assessment schedules for instructor-led classes.")
public class ClassAssessmentScheduleBatchController {

    public static final String API_ROOT_PATH = "/api/v1/classes/assessment-schedules";

    private final ClassAssessmentScheduleService classAssessmentScheduleService;

    @Operation(summary = "List assignment and quiz schedules for several classes",
            description = "class_uuids is comma-separated or repeated, 1 to " + ClassAssessmentScheduleService.MAX_BATCH_SIZE
                    + " entries. Classes the caller may not view (same rule as the per-class listings) are omitted. "
                    + "Costs a fixed number of queries per batch.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Assessment schedules retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No class uuids, or more than the maximum, requested")
    @GetMapping
    public ResponseEntity<ApiResponse<ClassAssessmentSchedulesDTO>> getAssessmentSchedules(
            @Parameter(description = "Class definition UUIDs, comma-separated", required = true)
            @RequestParam(value = "class_uuids", required = false) List<UUID> classUuids) {
        // Optional at binding only so a missing list reaches the service's 400 instead of the generic 500.
        ClassAssessmentSchedulesDTO result = classAssessmentScheduleService.getAssessmentSchedules(classUuids);
        return ResponseEntity.ok(ApiResponse.success(result, "Assessment schedules retrieved successfully"));
    }
}
