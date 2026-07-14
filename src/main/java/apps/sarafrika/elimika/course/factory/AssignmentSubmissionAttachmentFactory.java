package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.AssignmentSubmissionAttachmentDTO;
import apps.sarafrika.elimika.course.model.AssignmentSubmissionAttachment;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;

public class AssignmentSubmissionAttachmentFactory {

    private AssignmentSubmissionAttachmentFactory() {
    }

    public static AssignmentSubmissionAttachmentDTO toDTO(AssignmentSubmissionAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return new AssignmentSubmissionAttachmentDTO(
                attachment.getUuid(),
                attachment.getSubmissionUuid(),
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

    public static AssignmentSubmissionAttachment toEntity(AssignmentSubmissionAttachmentDTO dto) {
        if (dto == null) {
            return null;
        }

        AssignmentSubmissionAttachment attachment = new AssignmentSubmissionAttachment();
        attachment.setUuid(dto.uuid());
        attachment.setSubmissionUuid(dto.submissionUuid());
        attachment.setOriginalFilename(dto.originalFilename());
        attachment.setStoredFilename(FileUrlResolver.toStorableValue(dto.storedFilename()));
        attachment.setFileUrl(FileUrlResolver.toStorableValue(dto.fileUrl()));
        attachment.setFileSizeBytes(dto.fileSizeBytes());
        attachment.setMimeType(dto.mimeType());
        return attachment;
    }
}
