package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.CourseAssessmentGradeRevisionDTO;
import apps.sarafrika.elimika.course.dto.CourseEnrollmentFeedbackDTO;
import apps.sarafrika.elimika.course.dto.CourseGradeBandDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemScoreDTO;
import apps.sarafrika.elimika.course.dto.CourseGradeBookDTO;
import apps.sarafrika.elimika.course.dto.CourseProgressReportDTO;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import apps.sarafrika.elimika.course.internal.CourseProgressReportBuilder;
import apps.sarafrika.elimika.course.internal.LearnerAssessmentScope;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
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

@RestController
@RequestMapping(CourseGradeBookController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Course Gradebook", description = "Weighted gradebook components, linked tasks, and learner grade views")
@PreAuthorize(CourseGradeBookController.MANAGEMENT_ACCESS)
public class CourseGradeBookController {

    public static final String API_ROOT_PATH = "/api/v1/courses";
    /**
     * Marking somebody's work requires a real relationship to the course, not merely
     * holding the instructor or course_creator domain. The previous role-only check let
     * any instructor on the platform read and write any course's gradebook.
     */
    static final String MANAGEMENT_ACCESS =
            "@courseSecurityService.canManageCourseGradebook(#courseUuid) "
            + "or @domainSecurityService.isPlatformAdmin()";
    /**
     * Marks on one learner are awarded only by the default instructor of that learner's class.
     * A method-level {@code @PreAuthorize} replaces the class-level one, it is not ANDed with it.
     */
    static final String GRADE_ENROLLMENT_ACCESS =
            "@courseSecurityService.canGradeEnrollment(#courseUuid, #enrollmentUuid) "
            + "or @domainSecurityService.isPlatformAdmin()";
    /**
     * Reading one learner's marks: people around their class, the course author, or the learner.
     * The service still loads the enrolment by {@code findByUuidAndCourseUuid}.
     */
    static final String READ_ENROLLMENT_ACCESS =
            "@courseSecurityService.canReadEnrollmentGrades(#courseUuid, #enrollmentUuid) "
            + "or @domainSecurityService.isPlatformAdmin() "
            + "or @learnerAssessmentScope.ownsEnrollment(#enrollmentUuid)";

    private final CourseGradeBookService courseGradeBookService;
    private final DomainSecurityService domainSecurityService;
    private final LearnerAssessmentScope learnerAssessmentScope;
    private final CourseProgressReportBuilder progressReportBuilder;

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
    @PreAuthorize(GRADE_ENROLLMENT_ACCESS)
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
                scoreDTO.withGrader(domainSecurityService.getCurrentUserUuid())
        );
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(savedScore, "Gradebook line item score saved successfully"));
    }

    @Operation(summary = "Get line item rubric evaluation", description = "Returns the rubric evaluation for a learner against a rubric-backed gradebook line item.")
    @GetMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}/rubric-evaluations/{enrollmentUuid}")
    @PreAuthorize(READ_ENROLLMENT_ACCESS)
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
        if (evaluation.status() == CourseAssessmentLineItemRubricEvaluationStatus.PENDING
                && learnerAssessmentScope.ownsEnrollment(enrollmentUuid)) {
            evaluation = evaluation.asNotYetGraded();
        }
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(evaluation, "Gradebook line item rubric evaluation retrieved successfully"));
    }

    @Operation(summary = "Upsert line item rubric evaluation", description = "Completes or updates the rubric evaluation for a learner against a rubric-backed gradebook line item.")
    @PutMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}/rubric-evaluations/{enrollmentUuid}")
    @PreAuthorize(GRADE_ENROLLMENT_ACCESS)
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
                evaluationDTO.withGrader(domainSecurityService.getCurrentUserUuid())
        );
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(savedEvaluation, "Gradebook line item rubric evaluation saved successfully"));
    }

    @Operation(summary = "Get learner progress report", description = "Lesson-by-lesson report for one learner: what was taught, attendance, marks with criteria, final grade and label, and instructor feedback.")
    @GetMapping("/{courseUuid}/gradebook/enrollments/{enrollmentUuid}/progress-report")
    @PreAuthorize(READ_ENROLLMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseProgressReportDTO>> getProgressReport(
            @PathVariable UUID courseUuid,
            @PathVariable UUID enrollmentUuid
    ) {
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(
                progressReportBuilder.build(courseUuid, enrollmentUuid), "Progress report retrieved successfully"));
    }

    @Operation(summary = "List course grade bands", description = "The labels the course awards by final grade, highest minimum first.")
    @GetMapping("/{courseUuid}/grade-bands")
    @PreAuthorize("@courseSecurityService.canReadCourseContent(#courseUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseGradeBandDTO>>> getGradeBands(
            @PathVariable UUID courseUuid
    ) {
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(
                courseGradeBookService.getGradeBands(courseUuid), "Grade bands retrieved successfully"));
    }

    @Operation(summary = "Replace course grade bands", description = "Sets the course's grade bands; an empty list removes them. Only the course author may change grading policy.")
    @PutMapping("/{courseUuid}/grade-bands")
    @PreAuthorize("@courseSecurityService.isCourseOwner(#courseUuid) or @domainSecurityService.isPlatformAdmin()")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseGradeBandDTO>>> replaceGradeBands(
            @PathVariable UUID courseUuid,
            @Valid @RequestBody List<@Valid CourseGradeBandDTO> bands
    ) {
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(
                courseGradeBookService.replaceGradeBands(courseUuid, bands), "Grade bands saved successfully"));
    }

    @Operation(summary = "Add instructor feedback", description = "Records the class instructor's comment on a learner's progress. Earlier comments are kept.")
    @PostMapping("/{courseUuid}/gradebook/enrollments/{enrollmentUuid}/feedback")
    @PreAuthorize(GRADE_ENROLLMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseEnrollmentFeedbackDTO>> addEnrollmentFeedback(
            @PathVariable UUID courseUuid,
            @PathVariable UUID enrollmentUuid,
            @Valid @RequestBody CourseEnrollmentFeedbackDTO feedback
    ) {
        CourseEnrollmentFeedbackDTO saved = courseGradeBookService.addEnrollmentFeedback(
                courseUuid, enrollmentUuid, feedback.comments(), domainSecurityService.getCurrentUserUuid());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(saved, "Feedback saved successfully"));
    }

    @Operation(summary = "List instructor feedback", description = "Every instructor comment on a learner's progress, newest first.")
    @GetMapping("/{courseUuid}/gradebook/enrollments/{enrollmentUuid}/feedback")
    @PreAuthorize(READ_ENROLLMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseEnrollmentFeedbackDTO>>> getEnrollmentFeedback(
            @PathVariable UUID courseUuid,
            @PathVariable UUID enrollmentUuid
    ) {
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(
                courseGradeBookService.getEnrollmentFeedback(courseUuid, enrollmentUuid), "Feedback retrieved successfully"));
    }

    @Operation(summary = "List line item grade revisions", description = "Every grade given to a learner on a gradebook line item, oldest first, including regrades and earlier attempts.")
    @GetMapping("/{courseUuid}/assessments/{assessmentUuid}/line-items/{lineItemUuid}/revisions/{enrollmentUuid}")
    @PreAuthorize(READ_ENROLLMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseAssessmentGradeRevisionDTO>>> getLineItemRevisions(
            @PathVariable UUID courseUuid,
            @PathVariable UUID assessmentUuid,
            @PathVariable UUID lineItemUuid,
            @PathVariable UUID enrollmentUuid
    ) {
        List<CourseAssessmentGradeRevisionDTO> revisions = courseGradeBookService.getLineItemRevisions(
                courseUuid, assessmentUuid, lineItemUuid, enrollmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(revisions, "Grade revisions retrieved successfully"));
    }

    @Operation(summary = "Get enrollment gradebook", description = "Returns the weighted gradebook view for a learner in a course.")
    @GetMapping("/{courseUuid}/gradebook/enrollments/{enrollmentUuid}")
    @PreAuthorize(READ_ENROLLMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<CourseGradeBookDTO>> getEnrollmentGradeBook(
            @PathVariable UUID courseUuid,
            @PathVariable UUID enrollmentUuid
    ) {
        CourseGradeBookDTO gradeBook = courseGradeBookService.getEnrollmentGradeBook(courseUuid, enrollmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(gradeBook, "Course gradebook retrieved successfully"));
    }
}
