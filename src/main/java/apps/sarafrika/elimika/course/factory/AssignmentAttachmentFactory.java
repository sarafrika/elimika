package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.AssignmentAttachmentDTO;
import apps.sarafrika.elimika.course.model.AssignmentAttachment;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;

public class AssignmentAttachmentFactory {

    private AssignmentAttachmentFactory() {
    }

    public static AssignmentAttachmentDTO toDTO(AssignmentAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return new AssignmentAttachmentDTO(
                attachment.getUuid(),
                attachment.getAssignmentUuid(),
                attachment.getOriginalFilename(),
                attachment.getStoredFilename(),
                FileUrlResolver.publicUrl(attachment.getFileUrl() != null ? attachment.getFileUrl() : attachment.getStoredFilename()),
                attachment.getFileSizeBytes(),
                attachment.getMimeType(),
                attachment.getCreatedDate(),
                attachment.getCreatedBy(),
                attachment.getLastModifiedDate(),
                attachment.getLastModifiedBy()
        );
    }

    public static AssignmentAttachment toEntity(AssignmentAttachmentDTO dto) {
        if (dto == null) {
            return null;
        }

        AssignmentAttachment attachment = new AssignmentAttachment();
        attachment.setUuid(dto.uuid());
        attachment.setAssignmentUuid(dto.assignmentUuid());
        attachment.setOriginalFilename(dto.originalFilename());
        attachment.setStoredFilename(FileUrlResolver.toStorableValue(dto.storedFilename()));
        attachment.setFileUrl(FileUrlResolver.toStorableValue(dto.fileUrl()));
        attachment.setFileSizeBytes(dto.fileSizeBytes());
        attachment.setMimeType(dto.mimeType());
        return attachment;
    }
}
