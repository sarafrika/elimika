package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionAttachmentDTO;

import java.util.List;
import java.util.UUID;

public interface AssignmentSubmissionAttachmentService {
    AssignmentSubmissionAttachmentDTO createAttachment(AssignmentSubmissionAttachmentDTO attachmentDTO);

    AssignmentSubmissionAttachmentDTO getAttachmentByUuid(UUID uuid);

    List<AssignmentSubmissionAttachmentDTO> getAttachmentsBySubmission(UUID submissionUuid);

    void deleteAttachment(UUID uuid);
}
