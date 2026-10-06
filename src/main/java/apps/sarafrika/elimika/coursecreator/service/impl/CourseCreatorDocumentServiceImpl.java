package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDocumentDTO;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorDocumentService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileVerificationRequest;
import apps.sarafrika.elimika.profile.spi.UserDocumentDTO;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Course creator credential documents, kept on the user-owned professional profile. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorDocumentServiceImpl implements CourseCreatorDocumentService {

    private static final String NOT_FOUND = "Course creator document with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;
    private final DomainSecurityService domainSecurityService;

    @Override
    public CourseCreatorDocumentDTO createCourseCreatorDocument(CourseCreatorDocumentDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        String title = dto.title() == null || dto.title().isBlank() ? dto.originalFilename() : dto.title();
        return toDto(profileService.documents().create(userUuid, toUser(dto, title)), dto.courseCreatorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorDocumentDTO getCourseCreatorDocumentByUuid(UUID uuid) {
        UserDocumentDTO document = profileService.documents().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
        return toDto(document, bridge.findCourseCreatorUuid(document.userUuid()).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorDocumentDTO> getDocumentsByCourseCreatorUuid(UUID courseCreatorUuid) {
        return bridge.findUserUuid(courseCreatorUuid)
                .map(userUuid -> profileService.documents().list(userUuid).stream()
                        .map(document -> toDto(document, courseCreatorUuid)).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCreatorDocumentDTO> getVisibleDocumentsByCourseCreatorUuid(UUID courseCreatorUuid) {
        List<CourseCreatorDocumentDTO> documents = getDocumentsByCourseCreatorUuid(courseCreatorUuid);
        if (domainSecurityService.isCourseCreatorWithUuid(courseCreatorUuid) || domainSecurityService.isPlatformAdmin()) {
            return documents;
        }
        return documents.stream().filter(document -> Boolean.TRUE.equals(document.isVerified())).toList();
    }

    @Override
    public CourseCreatorDocumentDTO updateCourseCreatorDocument(UUID courseCreatorUuid, UUID uuid,
                                                                CourseCreatorDocumentDTO dto) {
        UUID userUuid = bridge.requireUserUuid(courseCreatorUuid);
        UserDocumentDTO existing = profileService.documents().find(uuid)
                .filter(document -> userUuid.equals(document.userUuid()))
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
        // Verification state is not writable here; it moves only through the admin verification.
        UserDocumentDTO saved = profileService.documents()
                .update(userUuid, uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, courseCreatorUuid);
    }

    @Override
    public CourseCreatorDocumentDTO verifyCourseCreatorDocument(UUID uuid, String verifiedBy, String verificationNotes) {
        UserDocumentDTO document = profileService.documents().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
        profileService.verify(document.userUuid(), ProfileSection.DOCUMENTS, uuid,
                new ProfileVerificationRequest(WalletVerificationStatus.VERIFIED, verificationNotes), verifiedBy);
        return getCourseCreatorDocumentByUuid(uuid);
    }

    @Override
    public void deleteCourseCreatorDocument(UUID courseCreatorUuid, UUID uuid) {
        profileService.documents().delete(bridge.requireUserUuid(courseCreatorUuid), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDocumentFileOf(UUID courseCreatorUuid, String filePath) {
        return bridge.findUserUuid(courseCreatorUuid)
                .map(userUuid -> profileService.ownsDocumentFile(userUuid, filePath))
                .orElse(false);
    }

    static UserDocumentDTO toUser(CourseCreatorDocumentDTO d) {
        return toUser(d, d.title());
    }

    static UserDocumentDTO toUser(CourseCreatorDocumentDTO d, String title) {
        return new UserDocumentDTO(null, null, d.documentTypeUuid(), d.educationUuid(), d.experienceUuid(),
                d.membershipUuid(), d.originalFilename(), d.storedFilename(), d.filePath(), d.fileSizeBytes(),
                d.mimeType(), d.fileHash(), title, d.description(), null, null, null, null, null, null,
                d.expiryDate(), null, null, null, null);
    }

    static CourseCreatorDocumentDTO toDto(UserDocumentDTO d, UUID courseCreatorUuid) {
        return new CourseCreatorDocumentDTO(d.uuid(), courseCreatorUuid, d.documentTypeUuid(), d.educationUuid(),
                d.experienceUuid(), d.membershipUuid(), d.originalFilename(), d.storedFilename(), d.filePath(),
                d.fileSizeBytes(), d.mimeType(), d.fileHash(), d.title(), d.description(), d.uploadDate(),
                d.isVerified(), d.verifiedBy(), d.verifiedAt(), d.verificationNotes(), d.status(), d.expiryDate(),
                d.createdDate(), d.createdBy(), d.updatedDate(), d.updatedBy());
    }
}
