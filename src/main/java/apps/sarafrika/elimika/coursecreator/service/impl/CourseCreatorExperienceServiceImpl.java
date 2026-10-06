package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorExperienceDTO;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorExperienceService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.UserExperienceDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Course creator experience records, kept on the user-owned professional profile shared by every domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorExperienceServiceImpl implements CourseCreatorExperienceService {

    private static final String NOT_FOUND = "Course creator experience with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;

    @Override
    public CourseCreatorExperienceDTO createCourseCreatorExperience(CourseCreatorExperienceDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        return toDto(profileService.experience().create(userUuid, toUser(dto)), dto.courseCreatorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorExperienceDTO getCourseCreatorExperienceByUuid(UUID uuid) {
        UserExperienceDTO item = require(uuid);
        return toDto(item, bridge.findCourseCreatorUuid(item.userUuid()).orElse(null));
    }

    /** Scoped to the course creator named in the payload, so a foreign item reads as not found. */
    @Override
    public CourseCreatorExperienceDTO updateCourseCreatorExperience(UUID uuid, CourseCreatorExperienceDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        UserExperienceDTO existing = require(uuid);
        UserExperienceDTO saved = profileService.experience().update(userUuid, uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, dto.courseCreatorUuid());
    }

    @Override
    public void deleteCourseCreatorExperience(UUID courseCreatorUuid, UUID uuid) {
        profileService.experience().delete(bridge.requireUserUuid(courseCreatorUuid), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorExperienceDTO> search(Map<String, String> searchParams, Pageable pageable) {
        CourseCreatorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserExperienceDTO> page = profileService.experience().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> creators = bridge.courseCreatorUuidsByUser(page.map(UserExperienceDTO::userUuid).toSet());
        return page.map(item -> toDto(item, creators.get(item.userUuid())));
    }

    private UserExperienceDTO require(UUID uuid) {
        return profileService.experience().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    static UserExperienceDTO toUser(CourseCreatorExperienceDTO d) {
        return new UserExperienceDTO(null, null, d.position(), d.organizationName(), d.responsibilities(),
                d.yearsOfExperience(), d.startDate(), d.endDate(), d.isCurrentPosition(), d.experienceType(), null, null,
                null, null);
    }

    static CourseCreatorExperienceDTO toDto(UserExperienceDTO u, UUID courseCreatorUuid) {
        return new CourseCreatorExperienceDTO(u.uuid(), courseCreatorUuid, u.position(), u.organizationName(),
                u.responsibilities(), u.yearsOfExperience(), u.startDate(), u.endDate(), u.isCurrentPosition(),
                u.experienceType(), u.createdDate(), u.createdBy(), u.updatedDate(), u.updatedBy());
    }
}
