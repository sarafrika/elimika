package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionAttachmentDTO;

import java.util.List;
import java.util.UUID;

public interface AssignmentSubmissionAttachmentService {
    AssignmentSubmissionAttachmentDTO createAttachment(AssignmentSubmissionAttachmentDTO attachmentDTO);

    AssignmentSubmissionAttachmentDTO getAttachmentByUuid(UUID uuid);

    List<AssignmentSubmissionAttachmentDTO> getAttachmentsBySubmission(UUID submissionUuid);

    /**
     * Removes one attachment from the submission it belongs to.
     * <p>
     * The submission is part of the address, not decoration: an attachment UUID on its own says
     * nothing about whose work it is, so a caller entitled to their own submission could name
     * anybody's file and have it deleted. Deleting is therefore refused unless the attachment is
     * actually filed under the submission named.
     *
     * @param submissionUuid the submission the attachment must belong to
     * @param attachmentUuid the attachment to remove
     */
    void deleteAttachment(UUID submissionUuid, UUID attachmentUuid);
}
