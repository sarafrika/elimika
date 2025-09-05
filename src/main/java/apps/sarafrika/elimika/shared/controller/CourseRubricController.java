package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.CourseRubricAssociationDTO;
import apps.sarafrika.elimika.course.service.CourseRubricAssociationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for managing course-rubric associations
 * <p>
 * Provides endpoints for associating rubrics with courses, supporting
 * rubric reuse across multiple courses and different usage contexts.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@RestController
@RequestMapping(CourseRubricController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Rubric Associations", description = "Management of rubric associations with courses for flexible assessment configurations")
public class CourseRubricController {

    public static final String API_ROOT_PATH = "/api/v1/courses/{courseUuid}/rubrics";

    private final CourseRubricAssociationService courseRubricAssociationService;

    @Operation(
            summary = "Associate a rubric with a course",
            description = "Creates an association between a rubric and a course, allowing the rubric to be used for assessments in that course."
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CourseRubricAssociationDTO>> associateRubric(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Valid @RequestBody CourseRubricAssociationDTO associationDTO) {
        
        // Ensure the course UUID in the path matches the DTO
        CourseRubricAssociationDTO correctedDTO = new CourseRubricAssociationDTO(
                associationDTO.uuid(),
                courseUuid, // Use path parameter
                associationDTO.rubricUuid(),
                associationDTO.associatedBy(),
                associationDTO.associationDate(),
                associationDTO.isPrimaryRubric(),
                associationDTO.usageContext(),
                associationDTO.createdDate(),
                associationDTO.createdBy(),
                associationDTO.updatedDate(),
                associationDTO.updatedBy()
        );
        
        CourseRubricAssociationDTO createdAssociation = courseRubricAssociationService.associateRubricWithCourse(correctedDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdAssociation, "Rubric associated with course successfully"));
    }

    @Operation(
            summary = "Get all rubrics associated with a course",
            description = "Retrieves all rubrics that are associated with the specified course, including usage context."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<CourseRubricAssociationDTO>>> getCourseRubrics(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            Pageable pageable) {
        
        Page<CourseRubricAssociationDTO> associations = courseRubricAssociationService.getRubricsByCourse(courseUuid, pageable);
        PagedDTO<CourseRubricAssociationDTO> pagedResponse = PagedDTO.from(associations, "/api/v1/courses/" + courseUuid + "/rubrics");
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, "Course rubrics retrieved successfully"));
    }

    @Operation(
            summary = "Get primary rubric for a course",
            description = "Retrieves the primary rubric association for the specified course."
    )
    @GetMapping(value = "/primary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CourseRubricAssociationDTO>> getPrimaryRubric(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid) {
        
        CourseRubricAssociationDTO primaryRubric = courseRubricAssociationService.getPrimaryRubricForCourse(courseUuid);
        if (primaryRubric != null) {
            return ResponseEntity.ok(ApiResponse.success(primaryRubric, "Primary rubric retrieved successfully"));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null, "No primary rubric found for this course"));
        }
    }

    @Operation(
            summary = "Set primary rubric for a course",
            description = "Designates a specific rubric as the primary rubric for the course."
    )
    @PutMapping(value = "/{rubricUuid}/primary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CourseRubricAssociationDTO>> setPrimaryRubric(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "UUID of the rubric to set as primary", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "UUID of the instructor making the change", required = true)
            @RequestParam UUID instructorUuid) {
        
        CourseRubricAssociationDTO primaryRubric = courseRubricAssociationService.setPrimaryRubric(courseUuid, rubricUuid, instructorUuid);
        return ResponseEntity.ok(ApiResponse.success(primaryRubric, "Primary rubric set successfully"));
    }

    @Operation(
            summary = "Get rubrics by usage context",
            description = "Retrieves rubric associations for a specific usage context (e.g., 'midterm', 'final', 'assignment')."
    )
    @GetMapping(value = "/context/{context}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagedDTO<CourseRubricAssociationDTO>>> getRubricsByContext(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "Usage context to filter by", required = true)
            @PathVariable String context,
            Pageable pageable) {
        
        Page<CourseRubricAssociationDTO> associations = courseRubricAssociationService.getAssociationsByContext(courseUuid, context, pageable);
        PagedDTO<CourseRubricAssociationDTO> pagedResponse = PagedDTO.from(associations, "/api/v1/courses/" + courseUuid + "/rubrics/context/" + context);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse, 
                String.format("Rubrics for context '%s' retrieved successfully", context)));
    }

    @Operation(
            summary = "Update rubric association",
            description = "Updates an existing rubric association, allowing changes to context, primary status, etc."
    )
    @PutMapping(value = "/associations/{associationUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CourseRubricAssociationDTO>> updateAssociation(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "UUID of the association to update", required = true)
            @PathVariable UUID associationUuid,
            @Valid @RequestBody CourseRubricAssociationDTO associationDTO) {
        
        CourseRubricAssociationDTO updatedAssociation = courseRubricAssociationService.updateAssociation(associationUuid, associationDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedAssociation, "Rubric association updated successfully"));
    }

    @Operation(
            summary = "Remove rubric association",
            description = "Removes the association between a rubric and a course."
    )
    @DeleteMapping(value = "/{rubricUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> dissociateRubric(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "UUID of the rubric to dissociate", required = true)
            @PathVariable UUID rubricUuid) {
        
        courseRubricAssociationService.dissociateRubricFromCourse(courseUuid, rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Rubric dissociated from course successfully"));
    }

    @Operation(
            summary = "Remove rubric association by context",
            description = "Removes a specific rubric association based on usage context."
    )
    @DeleteMapping(value = "/{rubricUuid}/context/{context}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> dissociateRubricByContext(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "UUID of the rubric to dissociate", required = true)
            @PathVariable UUID rubricUuid,
            @Parameter(description = "Usage context of the association to remove", required = true)
            @PathVariable String context) {
        
        courseRubricAssociationService.dissociateRubricFromCourseByContext(courseUuid, rubricUuid, context);
        return ResponseEntity.ok(ApiResponse.success(null, 
                String.format("Rubric dissociated from course for context '%s' successfully", context)));
    }

    @Operation(
            summary = "Check if rubric is associated with course",
            description = "Checks whether a specific rubric is already associated with the course."
    )
    @GetMapping(value = "/{rubricUuid}/exists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Boolean>> checkRubricAssociation(
            @Parameter(description = "UUID of the course", required = true)
            @PathVariable UUID courseUuid,
            @Parameter(description = "UUID of the rubric to check", required = true)
            @PathVariable UUID rubricUuid) {
        
        boolean isAssociated = courseRubricAssociationService.isRubricAssociatedWithCourse(courseUuid, rubricUuid);
        return ResponseEntity.ok(ApiResponse.success(isAssociated, 
                isAssociated ? "Rubric is associated with course" : "Rubric is not associated with course"));
    }
}