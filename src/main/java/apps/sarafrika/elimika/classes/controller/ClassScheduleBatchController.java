package apps.sarafrika.elimika.classes.controller;

import apps.sarafrika.elimika.classes.service.ClassScheduleBatchService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
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

/** Reads the scheduled sessions of many classes in one request instead of one schedule call per class. */
@RestController
@RequestMapping(ClassScheduleBatchController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Class Definition Management", description = "APIs for creating and managing class definitions and scheduling.")
public class ClassScheduleBatchController {

    public static final String API_ROOT_PATH = "/api/v1/classes/schedules";

    private final ClassScheduleBatchService classScheduleBatchService;

    @Operation(summary = "List the scheduled sessions of several classes",
            description = "class_uuids is comma-separated or repeated, 1 to " + ClassScheduleBatchService.MAX_BATCH_SIZE
                    + " entries. Returns every scheduled instance of those classes ordered by start time; group by "
                    + "class_definition_uuid. Unknown ids are omitted. Same access as GET /classes/{uuid}/schedule.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Class schedules retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No class uuids, or more than the maximum, requested")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledInstanceDTO>>> getClassSchedules(
            @Parameter(description = "Class definition UUIDs, comma-separated", required = true)
            @RequestParam(value = "class_uuids", required = false) List<UUID> classUuids) {
        // Optional at binding only so a missing list reaches the service's 400 instead of the generic 500.
        List<ScheduledInstanceDTO> result = classScheduleBatchService.getSchedules(classUuids);
        return ResponseEntity.ok(ApiResponse.success(result, "Class schedules retrieved successfully"));
    }
}
