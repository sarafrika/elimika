package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorProfessionalMembershipDTO;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorProfessionalMembershipService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.UserMembershipDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Course creator membership records, kept on the user-owned professional profile shared by every domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorProfessionalMembershipServiceImpl implements CourseCreatorProfessionalMembershipService {

    private static final String NOT_FOUND = "Course creator membership with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;

    @Override
    public CourseCreatorProfessionalMembershipDTO createCourseCreatorProfessionalMembership(CourseCreatorProfessionalMembershipDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        return toDto(profileService.memberships().create(userUuid, toUser(dto)), dto.courseCreatorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorProfessionalMembershipDTO getCourseCreatorProfessionalMembershipByUuid(UUID uuid) {
        UserMembershipDTO item = require(uuid);
        return toDto(item, bridge.findCourseCreatorUuid(item.userUuid()).orElse(null));
    }

    /** Scoped to the course creator named in the payload, so a foreign item reads as not found. */
    @Override
    public CourseCreatorProfessionalMembershipDTO updateCourseCreatorProfessionalMembership(UUID uuid, CourseCreatorProfessionalMembershipDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        UserMembershipDTO existing = require(uuid);
        UserMembershipDTO saved = profileService.memberships().update(userUuid, uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, dto.courseCreatorUuid());
    }

    @Override
    public void deleteCourseCreatorProfessionalMembership(UUID courseCreatorUuid, UUID uuid) {
        profileService.memberships().delete(bridge.requireUserUuid(courseCreatorUuid), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorProfessionalMembershipDTO> search(Map<String, String> searchParams, Pageable pageable) {
        CourseCreatorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserMembershipDTO> page = profileService.memberships().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> creators = bridge.courseCreatorUuidsByUser(page.map(UserMembershipDTO::userUuid).toSet());
        return page.map(item -> toDto(item, creators.get(item.userUuid())));
    }

    private UserMembershipDTO require(UUID uuid) {
        return profileService.memberships().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    static UserMembershipDTO toUser(CourseCreatorProfessionalMembershipDTO d) {
        return new UserMembershipDTO(null, null, d.organizationName(), d.membershipNumber(), d.startDate(), d.endDate(),
                d.isActive(), null, null, null, null);
    }

    static CourseCreatorProfessionalMembershipDTO toDto(UserMembershipDTO u, UUID courseCreatorUuid) {
        return new CourseCreatorProfessionalMembershipDTO(u.uuid(), courseCreatorUuid, u.organizationName(),
                u.membershipNumber(), u.startDate(), u.endDate(), u.isActive(), u.createdDate(), u.createdBy(),
                u.updatedDate(), u.updatedBy());
    }
}
