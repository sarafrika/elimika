package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorDocumentDTO;
import apps.sarafrika.elimika.instructor.model.InstructorDocument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorDocumentFactory {

    // Convert InstructorDocument entity to InstructorDocumentDTO
    public static InstructorDocumentDTO toDTO(InstructorDocument document) {
        if (document == null) {
            return null;
        }
        return new InstructorDocumentDTO(
                document.getUuid(),
                document.getInstructorUuid(),
                document.getDocumentTypeUuid(),
                document.getEducationUuid(),
                document.getExperienceUuid(),
                document.getMembershipUuid(),
                document.getOriginalFilename(),
                document.getStoredFilename(),
                document.getFilePath(),
                document.getFileSizeBytes(),
                document.getMimeType(),
                document.getFileHash(),
                document.getTitle(),
                document.getDescription(),
                document.getUploadDate(),
                document.getIsVerified(),
                document.getVerifiedBy(),
                document.getVerifiedAt(),
                document.getVerificationNotes(),
                document.getStatus(),
                document.getExpiryDate(),
                document.getCreatedDate(),
                document.getCreatedBy(),
                document.getLastModifiedDate(),
                document.getLastModifiedBy()
        );
    }

    // Convert InstructorDocumentDTO to InstructorDocument entity
    public static InstructorDocument toEntity(InstructorDocumentDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorDocument document = new InstructorDocument();
        document.setUuid(dto.uuid());
        document.setInstructorUuid(dto.instructorUuid());
        document.setDocumentTypeUuid(dto.documentTypeUuid());
        document.setEducationUuid(dto.educationUuid());
        document.setExperienceUuid(dto.experienceUuid());
        document.setMembershipUuid(dto.membershipUuid());
        document.setOriginalFilename(dto.originalFilename());
        document.setTitle(dto.title());
        document.setDescription(dto.description());
        document.setExpiryDate(dto.expiryDate());
        return document;
    }
}