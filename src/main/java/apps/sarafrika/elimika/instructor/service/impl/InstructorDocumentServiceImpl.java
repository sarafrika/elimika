package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorDocumentDTO;
import apps.sarafrika.elimika.instructor.internal.InstructorProfileBridge;
import apps.sarafrika.elimika.instructor.service.InstructorDocumentService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileVerificationRequest;
import apps.sarafrika.elimika.profile.spi.UserDocumentDTO;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Instructor credential documents, kept on the user-owned professional profile. */
@Service
@RequiredArgsConstructor
@Transactional
public class InstructorDocumentServiceImpl implements InstructorDocumentService {

    private static final String NOT_FOUND = "Instructor document with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final InstructorProfileBridge bridge;

    @Override
    public InstructorDocumentDTO createInstructorDocument(InstructorDocumentDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.instructorUuid());
        return toDto(profileService.documents().create(userUuid, toUser(dto)), dto.instructorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorDocumentDTO getInstructorDocumentByUuid(UUID uuid) {
        UserDocumentDTO document = require(uuid);
        return toDto(document, bridge.findInstructorUuid(document.userUuid()).orElse(null));
    }

    @Override
    public InstructorDocumentDTO updateInstructorDocument(UUID uuid, InstructorDocumentDTO dto) {
        UserDocumentDTO existing = require(uuid);
        UserDocumentDTO saved = profileService.documents()
                .update(existing.userUuid(), uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, bridge.findInstructorUuid(saved.userUuid()).orElse(dto.instructorUuid()));
    }

    @Override
    public void deleteInstructorDocument(UUID uuid) {
        profileService.documents().delete(require(uuid).userUuid(), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDocumentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        InstructorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserDocumentDTO> page = profileService.documents().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> instructors = bridge.instructorUuidsByUser(page.map(UserDocumentDTO::userUuid).toSet());
        return page.map(document -> toDto(document, instructors.get(document.userUuid())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDocumentDTO> getDocumentsByInstructorUuid(UUID instructorUuid) {
        return bridge.findUserUuid(instructorUuid)
                .map(userUuid -> profileService.documents().list(userUuid).stream()
                        .map(document -> toDto(document, instructorUuid)).toList())
                .orElse(List.of());
    }

    /** The verdict lands on the shared document, so it holds for every domain of its owner. */
    @Override
    public InstructorDocumentDTO verifyDocument(UUID uuid, String verifiedBy, String verificationNotes) {
        UserDocumentDTO document = require(uuid);
        profileService.verify(document.userUuid(), ProfileSection.DOCUMENTS, uuid,
                new ProfileVerificationRequest(WalletVerificationStatus.VERIFIED, verificationNotes), verifiedBy);
        return getInstructorDocumentByUuid(uuid);
    }

    /** Whether the stored file belongs to one of the instructor's documents, wherever it was uploaded. */
    @Override
    @Transactional(readOnly = true)
    public boolean isDocumentFileOf(UUID instructorUuid, String filePath) {
        return bridge.findUserUuid(instructorUuid)
                .map(userUuid -> profileService.ownsDocumentFile(userUuid, filePath))
                .orElse(false);
    }

    private UserDocumentDTO require(UUID uuid) {
        return profileService.documents().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    static UserDocumentDTO toUser(InstructorDocumentDTO d) {
        return new UserDocumentDTO(null, null, d.documentTypeUuid(), d.educationUuid(), d.experienceUuid(),
                d.membershipUuid(), d.originalFilename(), d.storedFilename(), d.filePath(), d.fileSizeBytes(),
                d.mimeType(), d.fileHash(), d.title(), d.description(), null, null, null, null, null, null,
                d.expiryDate(), null, null, null, null);
    }

    static InstructorDocumentDTO toDto(UserDocumentDTO d, UUID instructorUuid) {
        return new InstructorDocumentDTO(d.uuid(), instructorUuid, d.documentTypeUuid(), d.educationUuid(),
                d.experienceUuid(), d.membershipUuid(), d.originalFilename(), d.storedFilename(), d.filePath(),
                d.fileSizeBytes(), d.mimeType(), d.fileHash(), d.title(), d.description(), d.uploadDate(),
                d.isVerified(), d.verifiedBy(), d.verifiedAt(), d.verificationNotes(), d.status(), d.expiryDate(),
                d.createdDate(), d.createdBy(), d.updatedDate(), d.updatedBy());
    }
}
