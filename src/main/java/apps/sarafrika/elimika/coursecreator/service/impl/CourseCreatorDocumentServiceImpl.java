package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorDocumentFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorDocument;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorDocumentRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorDocumentService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorDocumentServiceImpl implements CourseCreatorDocumentService {

    private static final String DOCUMENT_NOT_FOUND_TEMPLATE = "Course creator document with ID %s not found";

    private final CourseCreatorDocumentRepository documentRepository;

    @Override
    public CourseCreatorDocumentDTO createCourseCreatorDocument(CourseCreatorDocumentDTO documentDTO) {
        CourseCreatorDocument document = CourseCreatorDocumentFactory.toEntity(documentDTO);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        document.setCreatedDate(now);
        document.setIsVerified(false);
        document.setVerifiedBy(null);
        document.setVerifiedAt(null);
        document.setVerificationNotes(null);
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

        return CourseCreatorDocumentFactory.toDTO(documentRepository.save(existing));
    }

    @Override
    public void deleteCourseCreatorDocument(UUID uuid) {
        if (!documentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(DOCUMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        documentRepository.deleteByUuid(uuid);
    }
}
