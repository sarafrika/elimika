package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorDocument;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseCreatorDocumentFactory {

    public static CourseCreatorDocumentDTO toDTO(CourseCreatorDocument document) {
        if (document == null) {
            return null;
        }
        return new CourseCreatorDocumentDTO(
                document.getUuid(),
                document.getCourseCreatorUuid(),
                document.getDocumentTypeUuid(),
                document.getEducationUuid(),
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

    public static CourseCreatorDocument toEntity(CourseCreatorDocumentDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseCreatorDocument document = new CourseCreatorDocument();
        document.setUuid(dto.uuid());
        document.setCourseCreatorUuid(dto.courseCreatorUuid());
        document.setDocumentTypeUuid(dto.documentTypeUuid());
        document.setEducationUuid(dto.educationUuid());
        document.setOriginalFilename(dto.originalFilename());
        document.setStoredFilename(dto.storedFilename());
        document.setFilePath(dto.filePath());
        document.setFileSizeBytes(dto.fileSizeBytes());
        document.setMimeType(dto.mimeType());
        document.setFileHash(dto.fileHash());
        document.setTitle(dto.title());
        document.setDescription(dto.description());
        document.setUploadDate(dto.uploadDate());
        document.setIsVerified(dto.isVerified());
        document.setVerifiedBy(dto.verifiedBy());
        document.setVerifiedAt(dto.verifiedAt());
        document.setVerificationNotes(dto.verificationNotes());
        document.setStatus(dto.status());
        document.setExpiryDate(dto.expiryDate());
        document.setCreatedDate(dto.createdDate());
        document.setCreatedBy(dto.createdBy());
        document.setLastModifiedDate(dto.updatedDate());
        document.setLastModifiedBy(dto.updatedBy());
        return document;
    }
}
