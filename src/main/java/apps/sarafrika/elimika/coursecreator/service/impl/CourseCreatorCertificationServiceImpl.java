package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCertificationDTO;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorCertificationService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.UserCertificationDTO;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Course creator certification records, kept on the user-owned professional profile shared by every domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorCertificationServiceImpl implements CourseCreatorCertificationService {

    private static final String NOT_FOUND = "Course creator certification with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;

    @Override
    public CourseCreatorCertificationDTO createCourseCreatorCertification(CourseCreatorCertificationDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        return toDto(profileService.certifications().create(userUuid, toUser(dto)), dto.courseCreatorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorCertificationDTO getCourseCreatorCertificationByUuid(UUID uuid) {
        UserCertificationDTO item = require(uuid);
        return toDto(item, bridge.findCourseCreatorUuid(item.userUuid()).orElse(null));
    }

    /** Scoped to the course creator named in the payload, so a foreign item reads as not found. */
    @Override
    public CourseCreatorCertificationDTO updateCourseCreatorCertification(UUID uuid, CourseCreatorCertificationDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        UserCertificationDTO existing = require(uuid);
        UserCertificationDTO saved = profileService.certifications().update(userUuid, uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, dto.courseCreatorUuid());
    }

    @Override
    public void deleteCourseCreatorCertification(UUID courseCreatorUuid, UUID uuid) {
        profileService.certifications().delete(bridge.requireUserUuid(courseCreatorUuid), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorCertificationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        CourseCreatorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserCertificationDTO> page = profileService.certifications().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> creators = bridge.courseCreatorUuidsByUser(page.map(UserCertificationDTO::userUuid).toSet());
        return page.map(item -> toDto(item, creators.get(item.userUuid())));
    }

    private UserCertificationDTO require(UUID uuid) {
        return profileService.certifications().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    static UserCertificationDTO toUser(CourseCreatorCertificationDTO d) {
        return new UserCertificationDTO(null, null, d.certificationName(), d.issuingOrganization(), d.issuedDate(),
                d.expiryDate(), d.credentialId(), d.credentialUrl(), d.description(), d.credentialType(), null, null, null,
                null, null, null, null);
    }

    static CourseCreatorCertificationDTO toDto(UserCertificationDTO u, UUID courseCreatorUuid) {
        return new CourseCreatorCertificationDTO(u.uuid(), courseCreatorUuid, u.certificationName(),
                u.issuingOrganization(), u.issuedDate(), u.expiryDate(), u.credentialId(), u.credentialUrl(),
                u.description(), u.credentialType(), u.verificationStatus() == WalletVerificationStatus.VERIFIED,
                u.createdDate(), u.createdBy(), u.updatedDate(), u.updatedBy());
    }
}
