package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionDTO;
import apps.sarafrika.elimika.course.dto.AssignmentSubmissionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AssignmentSubmissionService {

    // ===== BASIC CRUD OPERATIONS =====
    AssignmentSubmissionDTO createAssignmentSubmission(AssignmentSubmissionDTO assignmentSubmissionDTO);

    AssignmentSubmissionDTO getAssignmentSubmissionByUuid(UUID uuid);

    Page<AssignmentSubmissionDTO> getAllAssignmentSubmissions(Pageable pageable);

    AssignmentSubmissionDTO updateAssignmentSubmission(UUID uuid, AssignmentSubmissionDTO assignmentSubmissionDTO);

    void deleteAssignmentSubmission(UUID uuid);

    Page<AssignmentSubmissionDTO> search(Map<String, String> searchParams, Pageable pageable);

    // ===== SUBMISSION WORKFLOW OPERATIONS =====

    /**
     * Submit an assignment for a student.
     *
     * @param assignmentUuid The assignment UUID
     * @param request The submission request
     * @param hasUploadedFiles Whether multipart files are attached to the same request
     * @return Created or resubmitted submission DTO
     */
    AssignmentSubmissionDTO submitAssignment(UUID assignmentUuid,
                                             AssignmentSubmissionRequest request,
                                             boolean hasUploadedFiles);

    default AssignmentSubmissionDTO submitAssignment(UUID enrollmentUuid, UUID assignmentUuid,
                                                     String content, String[] fileUrls) {
        return submitAssignment(
                assignmentUuid,
                new AssignmentSubmissionRequest(enrollmentUuid, null, content, fileUrls),
                false
        );
    }

    /**
     * Get all submissions for a specific assignment
     * @param assignmentUuid The assignment UUID
     * @return List of submission DTOs
     */
    List<AssignmentSubmissionDTO> getSubmissionsByAssignment(UUID assignmentUuid);

    /**
     * Grade a submission
     * @param submissionUuid The submission UUID
     * @param score The score given
     * @param maxScore The maximum possible score
     * @param comments Optional instructor comments
     * @return Updated submission DTO
     */
    AssignmentSubmissionDTO gradeSubmission(UUID submissionUuid, BigDecimal score,
                                            BigDecimal maxScore, String comments);

    /**
     * Return submission for revision
     * @param submissionUuid The submission UUID
     * @param feedback Instructor feedback
     * @return Updated submission DTO
     */
    AssignmentSubmissionDTO returnForRevision(UUID submissionUuid, String feedback);

    // ===== ANALYTICS OPERATIONS =====

    /**
     * Get submission category distribution for an assignment
     * @param assignmentUuid The assignment UUID
     * @return Map of status categories and their counts
     */
    Map<String, Long> getSubmissionCategoryDistribution(UUID assignmentUuid);

    /**
     * Get average submission score for an assignment
     * @param assignmentUuid The assignment UUID
     * @return Average score as Double
     */
    Double getAverageSubmissionScore(UUID assignmentUuid);

    /**
     * Get high performance submissions (above 85%)
     * @param assignmentUuid The assignment UUID
     * @return List of high-performing submissions
     */
    List<AssignmentSubmissionDTO> getHighPerformanceSubmissions(UUID assignmentUuid);

    /**
     * Get pending grading submissions for an instructor
     * @param instructorUuid The instructor UUID
     * @return List of submissions pending grading
     */
    List<AssignmentSubmissionDTO> getPendingGrading(UUID instructorUuid);
}
