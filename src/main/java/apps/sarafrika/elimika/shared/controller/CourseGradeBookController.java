package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemScoreDTO;
import apps.sarafrika.elimika.course.dto.CourseGradeBookDTO;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping(CourseGradeBookController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Gradebook", description = "Weighted gradebook components, linked tasks, and learner grade views")
public class CourseGradeBookController {

    public static final String API_ROOT_PATH = "/api/v1/courses";

    private final CourseGradeBookService courseGradeBookService;

    @Operation(summary = "Create gradebook line item", description = "Adds a linked task under a weighted course assessment component.")
    @PostMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseAssessmentLineItemDTO>> createLineItem(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @Valid @RequestBody CourseAssessmentLineItemDTO lineItemDTO
    ) {
        CourseAssessmentLineItemDTO createdLineItem = courseGradeBookService.createLineItem(courseUuid, assessmentUuid, lineItemDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(createdLineItem, "Gradebook line item created successfully"));
    }

    @Operation(summary = "List gradebook line items", description = "Returns linked tasks configured under a weighted course assessment component.")
    @GetMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseAssessmentLineItemDTO>>> getLineItems(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid
    ) {
        List<CourseAssessmentLineItemDTO> lineItems = courseGradeBookService.getLineItems(courseUuid, assessmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(lineItems, "Gradebook line items retrieved successfully"));
    }

    @Operation(summary = "Update gradebook line item", description = "Updates a linked task inside a weighted course assessment component.")
    @PutMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseAssessmentLineItemDTO>> updateLineItem(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @PathVariable UUID lineItemUuid,
            @Valid @RequestBody CourseAssessmentLineItemDTO lineItemDTO
    ) {
        CourseAssessmentLineItemDTO updatedLineItem = courseGradeBookService.updateLineItem(
                courseUuid,
                assessmentUuid,
                lineItemUuid,
                lineItemDTO
        );
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(updatedLineItem, "Gradebook line item updated successfully"));
    }

    @Operation(summary = "Delete gradebook line item", description = "Removes a linked task from a weighted course assessment component.")
    @DeleteMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}")
    public ResponseEntity<Void> deleteLineItem(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @PathVariable UUID lineItemUuid
    ) {
        courseGradeBookService.deleteLineItem(courseUuid, assessmentUuid, lineItemUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upsert line item score", description = "Records or updates a learner score for a linked gradebook task.")
    @PutMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}/scores/{enrollmentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseAssessmentLineItemScoreDTO>> upsertLineItemScore(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @PathVariable UUID lineItemUuid,
            @PathVariable UUID enrollmentUuid,
            @Valid @RequestBody CourseAssessmentLineItemScoreDTO scoreDTO
    ) {
        CourseAssessmentLineItemScoreDTO savedScore = courseGradeBookService.upsertLineItemScore(
                courseUuid,
                assessmentUuid,
                lineItemUuid,
                enrollmentUuid,
                scoreDTO
        );
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(savedScore, "Gradebook line item score saved successfully"));
    }

    @Operation(summary = "Get line item rubric evaluation", description = "Returns the rubric evaluation for a learner against a rubric-backed gradebook line item.")
    @GetMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}/rubric-evaluations/{enrollmentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseAssessmentLineItemRubricEvaluationDTO>> getLineItemRubricEvaluation(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @PathVariable UUID lineItemUuid,
            @PathVariable UUID enrollmentUuid
    ) {
        CourseAssessmentLineItemRubricEvaluationDTO evaluation = courseGradeBookService.getLineItemRubricEvaluation(
                courseUuid,
                assessmentUuid,
                lineItemUuid,
                enrollmentUuid
        );
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(evaluation, "Gradebook line item rubric evaluation retrieved successfully"));
    }

    @Operation(summary = "Upsert line item rubric evaluation", description = "Completes or updates the rubric evaluation for a learner against a rubric-backed gradebook line item.")
    @PutMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}/rubric-evaluations/{enrollmentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseAssessmentLineItemRubricEvaluationDTO>> upsertLineItemRubricEvaluation(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @PathVariable UUID lineItemUuid,
            @PathVariable UUID enrollmentUuid,
            @Valid @RequestBody CourseAssessmentLineItemRubricEvaluationDTO evaluationDTO
    ) {
        CourseAssessmentLineItemRubricEvaluationDTO savedEvaluation = courseGradeBookService.upsertLineItemRubricEvaluation(
                courseUuid,
                assessmentUuid,
                lineItemUuid,
                enrollmentUuid,
                evaluationDTO
        );
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(savedEvaluation, "Gradebook line item rubric evaluation saved successfully"));
    }

    @Operation(summary = "Get enrollment gradebook", description = "Returns the weighted gradebook view for a learner in a course.")
    @GetMapping("/{courseUuid}/gradebook/enrollments/{enrollmentUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseGradeBookDTO>> getEnrollmentGradeBook(
            @PathVariable UUID courseUuid,
            @PathVariable UUID enrollmentUuid
    ) {
        CourseGradeBookDTO gradeBook = courseGradeBookService.getEnrollmentGradeBook(courseUuid, enrollmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(gradeBook, "Course gradebook retrieved successfully"));
    }
}
