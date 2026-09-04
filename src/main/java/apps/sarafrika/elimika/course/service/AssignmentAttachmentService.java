package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssignmentAttachmentDTO;

import java.util.List;
import java.util.UUID;

public interface AssignmentAttachmentService {
    AssignmentAttachmentDTO createAttachment(AssignmentAttachmentDTO attachmentDTO);

    AssignmentAttachmentDTO getAttachmentByUuid(UUID uuid);

    List<AssignmentAttachmentDTO> getAttachmentsByAssignment(UUID assignmentUuid);

    /**
     * Removes one attachment from the assignment it belongs to.
     * <p>
     * The assignment is part of the address. Its guard is what decides whether the caller may
     * change this assignment's material, so an attachment left unbound to it would let anyone
     * holding one assignment delete the briefs and datasets hanging off every other.
     *
     * @param assignmentUuid the assignment the attachment must belong to
     * @param attachmentUuid the attachment to remove
     */
    void deleteAttachment(UUID assignmentUuid, UUID attachmentUuid);
}
