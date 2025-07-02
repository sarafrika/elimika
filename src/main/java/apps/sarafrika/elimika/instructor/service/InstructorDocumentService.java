package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.InstructorDocumentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InstructorDocumentService {
    InstructorDocumentDTO createInstructorDocument(InstructorDocumentDTO instructorDocumentDTO);
    InstructorDocumentDTO getInstructorDocumentByUuid(UUID uuid);
    Page<InstructorDocumentDTO> getAllInstructorDocuments(Pageable pageable);
    InstructorDocumentDTO updateInstructorDocument(UUID uuid, InstructorDocumentDTO instructorDocumentDTO);
    void deleteInstructorDocument(UUID uuid);
    Page<InstructorDocumentDTO> search(Map<String, String> searchParams, Pageable pageable);

    // Additional methods specific to InstructorDocument
    List<InstructorDocumentDTO> getDocumentsByInstructorUuid(UUID instructorUuid);
    List<InstructorDocumentDTO> getDocumentsByDocumentTypeUuid(UUID documentTypeUuid);
    List<InstructorDocumentDTO> getDocumentsByInstructorAndDocumentType(UUID instructorUuid, UUID documentTypeUuid);
    InstructorDocumentDTO verifyDocument(UUID uuid, String verifiedBy, String verificationNotes);
    void markDocumentAsExpired(UUID uuid);
    List<InstructorDocumentDTO> getExpiringDocuments(int daysBeforeExpiry);
    List<InstructorDocumentDTO> getUnverifiedDocuments();
}