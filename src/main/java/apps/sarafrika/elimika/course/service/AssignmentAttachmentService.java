package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.AssignmentAttachmentDTO;

import java.util.List;
import java.util.UUID;

public interface AssignmentAttachmentService {
    AssignmentAttachmentDTO createAttachment(AssignmentAttachmentDTO attachmentDTO);

    AssignmentAttachmentDTO getAttachmentByUuid(UUID uuid);

    List<AssignmentAttachmentDTO> getAttachmentsByAssignment(UUID assignmentUuid);

    void deleteAttachment(UUID uuid);
}
