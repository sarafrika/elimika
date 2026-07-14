package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorDocumentFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorDocument;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorDocumentRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorDocumentService;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorDocumentServiceImpl implements CourseCreatorDocumentService {

    private static final String DOCUMENT_NOT_FOUND_TEMPLATE = "Course creator document with ID %s not found";

    private final CourseCreatorDocumentRepository documentRepository;
    private final MediaStorageService mediaStorageService;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CourseCreatorDocumentDTO createCourseCreatorDocument(CourseCreatorDocumentDTO documentDTO) {
        CourseCreatorDocument document = CourseCreatorDocumentFactory.toEntity(documentDTO);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        document.setCreatedDate(now);
        document.setUploadDate(now);
        if (document.getStatus() == null) {
            document.setStatus(DocumentStatus.PENDING);
        }
        if (document.getTitle() == null || document.getTitle().isBlank()) {
            document.setTitle(document.getOriginalFilename());
        }
        if (document.getIsVerified() == null) {
            document.setIsVerified(false);
        }
        if (!Boolean.TRUE.equals(document.getIsVerified())) {
            document.setVerifiedBy(null);
            document.setVerifiedAt(null);
            document.setVerificationNotes(null);
        }
        return CourseCreatorDocumentFactory.toDTO(documentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorDocumentDTO getCourseCreatorDocumentByUuid(UUID uuid) {
        return documentRepository.findByUuid(uuid)
                .map(CourseCreatorDocumentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorDocumentDTO> getDocumentsByCourseCreatorUuid(UUID courseCreatorUuid) {
        return documentRepository.findByCourseCreatorUuid(courseCreatorUuid)
                .stream()
                .map(CourseCreatorDocumentFactory::toDTO)
                .toList();
    }

    @Override
    public CourseCreatorDocumentDTO updateCourseCreatorDocument(UUID uuid, CourseCreatorDocumentDTO documentDTO) {
        CourseCreatorDocument existing = documentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));

        if (documentDTO.courseCreatorUuid() != null) {
            existing.setCourseCreatorUuid(documentDTO.courseCreatorUuid());
        }
        if (documentDTO.documentTypeUuid() != null) {
            existing.setDocumentTypeUuid(documentDTO.documentTypeUuid());
        }
        if (documentDTO.educationUuid() != null) {
            existing.setEducationUuid(documentDTO.educationUuid());
        }
        if (documentDTO.experienceUuid() != null) {
            existing.setExperienceUuid(documentDTO.experienceUuid());
        }
        if (documentDTO.membershipUuid() != null) {
            existing.setMembershipUuid(documentDTO.membershipUuid());
        }
        if (documentDTO.originalFilename() != null) {
            existing.setOriginalFilename(documentDTO.originalFilename());
        }
        if (documentDTO.storedFilename() != null) {
            existing.setStoredFilename(documentDTO.storedFilename());
        }
        if (documentDTO.filePath() != null) {
            existing.setFilePath(documentDTO.filePath());
        }
        if (documentDTO.fileSizeBytes() != null) {
            existing.setFileSizeBytes(documentDTO.fileSizeBytes());
        }
        if (documentDTO.mimeType() != null) {
            existing.setMimeType(documentDTO.mimeType());
        }
        if (documentDTO.fileHash() != null) {
            existing.setFileHash(documentDTO.fileHash());
        }
        if (documentDTO.title() != null) {
            existing.setTitle(documentDTO.title());
        }
        if (documentDTO.description() != null) {
            existing.setDescription(documentDTO.description());
        }
        if (documentDTO.status() != null) {
            existing.setStatus(documentDTO.status());
        }
        if (documentDTO.expiryDate() != null) {
            existing.setExpiryDate(documentDTO.expiryDate());
        }
        if (documentDTO.isVerified() != null) {
            existing.setIsVerified(documentDTO.isVerified());
        }
        if (documentDTO.verifiedBy() != null) {
            existing.setVerifiedBy(documentDTO.verifiedBy());
        }
        if (documentDTO.verifiedAt() != null) {
            existing.setVerifiedAt(documentDTO.verifiedAt());
        }
        if (documentDTO.verificationNotes() != null) {
            existing.setVerificationNotes(documentDTO.verificationNotes());
        }

        return CourseCreatorDocumentFactory.toDTO(documentRepository.save(existing));
    }

    @Override
    public CourseCreatorDocumentDTO verifyCourseCreatorDocument(UUID uuid, String verifiedBy, String verificationNotes) {
        CourseCreatorDocument existing = documentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));

        existing.setIsVerified(true);
        existing.setVerifiedBy(verifiedBy);
        existing.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        existing.setVerificationNotes(verificationNotes);
        existing.setStatus(DocumentStatus.APPROVED);

        CourseCreatorDocument saved = documentRepository.save(existing);
        publishDocumentVerifiedNotification(saved);
        return CourseCreatorDocumentFactory.toDTO(saved);
    }

    @Override
    public void deleteCourseCreatorDocument(UUID uuid) {
        CourseCreatorDocument document = documentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));
        mediaStorageService.delete(document.getFilePath() != null
                ? document.getFilePath() : document.getStoredFilename());
        documentRepository.deleteByUuid(uuid);
    }

    private void publishDocumentVerifiedNotification(CourseCreatorDocument document) {
        if (document.getCourseCreatorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = courseCreatorLookupService.getCourseCreatorUserUuid(document.getCourseCreatorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        String documentTitle = document.getTitle() == null ? "Your document" : document.getTitle();
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "PROFILE_DOCUMENT_VERIFIED",
                "POPUP",
                "Document verified",
                documentTitle + " has been verified.",
                "/dashboard/profile/documents",
                Map.of(
                        "document_uuid", document.getUuid(),
                        "document_title", documentTitle,
                        "profile_type", "course_creator"
                ),
                "profile-document-verified:course-creator:" + document.getUuid()
        ));
    }
}
