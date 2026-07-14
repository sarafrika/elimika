package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorDocumentDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorDocumentFactory;
import apps.sarafrika.elimika.instructor.model.InstructorDocument;
import apps.sarafrika.elimika.instructor.repository.InstructorDocumentRepository;
import apps.sarafrika.elimika.instructor.service.InstructorDocumentService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorDocumentServiceImpl implements InstructorDocumentService {

    private final InstructorDocumentRepository instructorDocumentRepository;
    private final GenericSpecificationBuilder<InstructorDocument> specificationBuilder;
    private final InstructorLookupService instructorLookupService;
    private final ApplicationEventPublisher eventPublisher;
    private final MediaStorageService mediaStorageService;

    private static final String INSTRUCTOR_DOCUMENT_NOT_FOUND_TEMPLATE = "Instructor document with ID %s not found";

    @Override
    public InstructorDocumentDTO createInstructorDocument(InstructorDocumentDTO instructorDocumentDTO) {
        InstructorDocument instructorDocument = InstructorDocumentFactory.toEntity(instructorDocumentDTO);
        instructorDocument.setCreatedDate(LocalDateTime.now());
        instructorDocument.setUploadDate(LocalDateTime.now());

        // Set default values
        if (instructorDocument.getStatus() == null) {
            instructorDocument.setStatus(DocumentStatus.PENDING);
        }
        if (instructorDocument.getIsVerified() == null) {
            instructorDocument.setIsVerified(false);
        }

        InstructorDocument savedDocument = instructorDocumentRepository.save(instructorDocument);
        return InstructorDocumentFactory.toDTO(savedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorDocumentDTO getInstructorDocumentByUuid(UUID uuid) {
        return instructorDocumentRepository.findByUuid(uuid)
                .map(InstructorDocumentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDocumentDTO> getAllInstructorDocuments(Pageable pageable) {
        return instructorDocumentRepository.findAll(pageable).map(InstructorDocumentFactory::toDTO);
    }

    @Override
    public InstructorDocumentDTO updateInstructorDocument(UUID uuid, InstructorDocumentDTO instructorDocumentDTO) {
        InstructorDocument existingDocument = instructorDocumentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));

        // Update fields from DTO
        updateDocumentFields(existingDocument, instructorDocumentDTO);

        InstructorDocument updatedDocument = instructorDocumentRepository.save(existingDocument);
        return InstructorDocumentFactory.toDTO(updatedDocument);
    }

    @Override
    public void deleteInstructorDocument(UUID uuid) {
        InstructorDocument document = instructorDocumentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));
        mediaStorageService.delete(document.getFilePath() != null
                ? document.getFilePath() : document.getStoredFilename());
        instructorDocumentRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDocumentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<InstructorDocument> spec = specificationBuilder.buildSpecification(InstructorDocument.class, searchParams);
        return instructorDocumentRepository.findAll(spec, pageable).map(InstructorDocumentFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDocumentDTO> getDocumentsByInstructorUuid(UUID instructorUuid) {
        return instructorDocumentRepository.findByInstructorUuid(instructorUuid)
                .stream()
                .map(InstructorDocumentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDocumentDTO> getDocumentsByDocumentTypeUuid(UUID documentTypeUuid) {
        return instructorDocumentRepository.findByDocumentTypeUuid(documentTypeUuid)
                .stream()
                .map(InstructorDocumentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDocumentDTO> getDocumentsByInstructorAndDocumentType(UUID instructorUuid, UUID documentTypeUuid) {
        return instructorDocumentRepository.findByInstructorUuidAndDocumentTypeUuid(instructorUuid, documentTypeUuid)
                .stream()
                .map(InstructorDocumentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InstructorDocumentDTO verifyDocument(UUID uuid, String verifiedBy, String verificationNotes) {
        InstructorDocument document = instructorDocumentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));

        document.setIsVerified(true);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now());
        document.setVerificationNotes(verificationNotes);
        document.setStatus(DocumentStatus.APPROVED);

        InstructorDocument updatedDocument = instructorDocumentRepository.save(document);
        publishDocumentVerifiedNotification(updatedDocument);
        return InstructorDocumentFactory.toDTO(updatedDocument);
    }

    @Override
    public void markDocumentAsExpired(UUID uuid) {
        InstructorDocument document = instructorDocumentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_DOCUMENT_NOT_FOUND_TEMPLATE, uuid)));

        document.setStatus(DocumentStatus.EXPIRED);
        instructorDocumentRepository.save(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDocumentDTO> getExpiringDocuments(int daysBeforeExpiry) {
        LocalDate cutoffDate = LocalDate.now().plusDays(daysBeforeExpiry);
        return instructorDocumentRepository.findByExpiryDateBeforeAndStatusNot(cutoffDate, DocumentStatus.EXPIRED)
                .stream()
                .map(InstructorDocumentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDocumentDTO> getUnverifiedDocuments() {
        return instructorDocumentRepository.findByIsVerifiedFalse()
                .stream()
                .map(InstructorDocumentFactory::toDTO)
                .collect(Collectors.toList());
    }

    private void updateDocumentFields(InstructorDocument existingDocument, InstructorDocumentDTO dto) {
        if (dto.instructorUuid() != null) {
            existingDocument.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.documentTypeUuid() != null) {
            existingDocument.setDocumentTypeUuid(dto.documentTypeUuid());
        }
        if (dto.educationUuid() != null) {
            existingDocument.setEducationUuid(dto.educationUuid());
        }
        if (dto.experienceUuid() != null) {
            existingDocument.setExperienceUuid(dto.experienceUuid());
        }
        if (dto.membershipUuid() != null) {
            existingDocument.setMembershipUuid(dto.membershipUuid());
        }
        if (dto.originalFilename() != null) {
            existingDocument.setOriginalFilename(dto.originalFilename());
        }
        if (dto.storedFilename() != null) {
            existingDocument.setStoredFilename(dto.storedFilename());
        }
        if (dto.filePath() != null) {
            existingDocument.setFilePath(dto.filePath());
        }
        if (dto.fileSizeBytes() != null) {
            existingDocument.setFileSizeBytes(dto.fileSizeBytes());
        }
        if (dto.mimeType() != null) {
            existingDocument.setMimeType(dto.mimeType());
        }
        if (dto.fileHash() != null) {
            existingDocument.setFileHash(dto.fileHash());
        }
        if (dto.title() != null) {
            existingDocument.setTitle(dto.title());
        }
        if (dto.description() != null) {
            existingDocument.setDescription(dto.description());
        }
        if (dto.status() != null) {
            existingDocument.setStatus(dto.status());
        }
        if (dto.expiryDate() != null) {
            existingDocument.setExpiryDate(dto.expiryDate());
        }
        if (dto.isVerified() != null) {
            existingDocument.setIsVerified(dto.isVerified());
        }
        if (dto.verifiedBy() != null) {
            existingDocument.setVerifiedBy(dto.verifiedBy());
        }
        if (dto.verifiedAt() != null) {
            existingDocument.setVerifiedAt(dto.verifiedAt());
        }
        if (dto.verificationNotes() != null) {
            existingDocument.setVerificationNotes(dto.verificationNotes());
        }
    }

    private void publishDocumentVerifiedNotification(InstructorDocument document) {
        if (document.getInstructorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = instructorLookupService.getInstructorUserUuid(document.getInstructorUuid())
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
                        "profile_type", "instructor"
                ),
                "profile-document-verified:instructor:" + document.getUuid()
        ));
    }
}
