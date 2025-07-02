package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionDTO;
import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssignmentSubmissionFactory {

    // Convert AssignmentSubmission entity to AssignmentSubmissionDTO
    public static AssignmentSubmissionDTO toDTO(AssignmentSubmission assignmentSubmission) {
        if (assignmentSubmission == null) {
            return null;
        }
        return new AssignmentSubmissionDTO(
                assignmentSubmission.getUuid(),
                assignmentSubmission.getEnrollmentUuid(),
                assignmentSubmission.getAssignmentUuid(),
                assignmentSubmission.getSubmissionText(),
                assignmentSubmission.getFileUrls(),
                assignmentSubmission.getSubmittedAt(),
                assignmentSubmission.getStatus(),
                assignmentSubmission.getScore(),
                assignmentSubmission.getMaxScore(),
                assignmentSubmission.getPercentage(),
                assignmentSubmission.getInstructorComments(),
                assignmentSubmission.getGradedAt(),
                assignmentSubmission.getGradedByUuid(),
                assignmentSubmission.getCreatedDate(),
                assignmentSubmission.getCreatedBy(),
                assignmentSubmission.getLastModifiedDate(),
                assignmentSubmission.getLastModifiedBy()
        );
    }

    // Convert AssignmentSubmissionDTO to AssignmentSubmission entity
    public static AssignmentSubmission toEntity(AssignmentSubmissionDTO dto) {
        if (dto == null) {
            return null;
        }
        AssignmentSubmission assignmentSubmission = new AssignmentSubmission();
        assignmentSubmission.setUuid(dto.uuid());
        assignmentSubmission.setEnrollmentUuid(dto.enrollmentUuid());
        assignmentSubmission.setAssignmentUuid(dto.assignmentUuid());
        assignmentSubmission.setSubmissionText(dto.submissionText());
        assignmentSubmission.setFileUrls(dto.fileUrls());
        assignmentSubmission.setSubmittedAt(dto.submittedAt());
        assignmentSubmission.setStatus(dto.status());
        assignmentSubmission.setScore(dto.score());
        assignmentSubmission.setMaxScore(dto.maxScore());
        assignmentSubmission.setPercentage(dto.percentage());
        assignmentSubmission.setInstructorComments(dto.instructorComments());
        assignmentSubmission.setGradedAt(dto.gradedAt());
        assignmentSubmission.setGradedByUuid(dto.gradedByUuid());
        assignmentSubmission.setCreatedDate(dto.createdDate());
        assignmentSubmission.setCreatedBy(dto.createdBy());
        assignmentSubmission.setLastModifiedDate(dto.updatedDate());
        assignmentSubmission.setLastModifiedBy(dto.updatedBy());
        return assignmentSubmission;
    }
}