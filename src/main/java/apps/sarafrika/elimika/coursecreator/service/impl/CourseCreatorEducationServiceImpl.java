package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorEducationDTO;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorEducationService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.UserEducationDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Course creator education records, kept on the user-owned professional profile shared by every domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorEducationServiceImpl implements CourseCreatorEducationService {

    private static final String NOT_FOUND = "Course creator education with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;

    @Override
    public CourseCreatorEducationDTO createCourseCreatorEducation(CourseCreatorEducationDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        return toDto(profileService.education().create(userUuid, toUser(dto)), dto.courseCreatorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorEducationDTO getCourseCreatorEducationByUuid(UUID uuid) {
        UserEducationDTO item = require(uuid);
        return toDto(item, bridge.findCourseCreatorUuid(item.userUuid()).orElse(null));
    }

    /** Scoped to the course creator named in the payload, so a foreign item reads as not found. */
    @Override
    public CourseCreatorEducationDTO updateCourseCreatorEducation(UUID uuid, CourseCreatorEducationDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        UserEducationDTO existing = require(uuid);
        UserEducationDTO saved = profileService.education().update(userUuid, uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, dto.courseCreatorUuid());
    }

    @Override
    public void deleteCourseCreatorEducation(UUID courseCreatorUuid, UUID uuid) {
        profileService.education().delete(bridge.requireUserUuid(courseCreatorUuid), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorEducationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        CourseCreatorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserEducationDTO> page = profileService.education().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> creators = bridge.courseCreatorUuidsByUser(page.map(UserEducationDTO::userUuid).toSet());
        return page.map(item -> toDto(item, creators.get(item.userUuid())));
    }

    private UserEducationDTO require(UUID uuid) {
        return profileService.education().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    static UserEducationDTO toUser(CourseCreatorEducationDTO d) {
        return new UserEducationDTO(null, null, d.qualification(), d.fieldOfStudy(), d.schoolName(), d.startYear(),
                d.yearCompleted(), d.certificateNumber(), null, null, null, null);
    }

    static CourseCreatorEducationDTO toDto(UserEducationDTO u, UUID courseCreatorUuid) {
        return new CourseCreatorEducationDTO(u.uuid(), courseCreatorUuid, u.qualification(), u.fieldOfStudy(), u.schoolName(),
                u.startYear(), u.yearCompleted(), u.certificateNumber(), u.createdDate(), u.createdBy(), u.updatedDate(),
                u.updatedBy());
    }
}
