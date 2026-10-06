package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorSkillDTO;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorSkillService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Course creator skill records, kept on the user-owned professional profile shared by every domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCreatorSkillServiceImpl implements CourseCreatorSkillService {

    private static final String NOT_FOUND = "Course creator skill with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final CourseCreatorProfileBridge bridge;

    @Override
    public CourseCreatorSkillDTO createCourseCreatorSkill(CourseCreatorSkillDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        return toDto(profileService.skills().create(userUuid, toUser(dto)), dto.courseCreatorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorSkillDTO getCourseCreatorSkillByUuid(UUID uuid) {
        UserSkillDTO item = require(uuid);
        return toDto(item, bridge.findCourseCreatorUuid(item.userUuid()).orElse(null));
    }

    /** Scoped to the course creator named in the payload, so a foreign item reads as not found. */
    @Override
    public CourseCreatorSkillDTO updateCourseCreatorSkill(UUID uuid, CourseCreatorSkillDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.courseCreatorUuid());
        UserSkillDTO existing = require(uuid);
        UserSkillDTO saved = profileService.skills().update(userUuid, uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, dto.courseCreatorUuid());
    }

    @Override
    public void deleteCourseCreatorSkill(UUID courseCreatorUuid, UUID uuid) {
        profileService.skills().delete(bridge.requireUserUuid(courseCreatorUuid), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorSkillDTO> search(Map<String, String> searchParams, Pageable pageable) {
        CourseCreatorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserSkillDTO> page = profileService.skills().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> creators = bridge.courseCreatorUuidsByUser(page.map(UserSkillDTO::userUuid).toSet());
        return page.map(item -> toDto(item, creators.get(item.userUuid())));
    }

    private UserSkillDTO require(UUID uuid) {
        return profileService.skills().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    static UserSkillDTO toUser(CourseCreatorSkillDTO d) {
        return UserSkillDTO.claim(d.skillName(), d.proficiencyLevel(), d.evidence(), d.lastAssessedOn());
    }

    static CourseCreatorSkillDTO toDto(UserSkillDTO u, UUID courseCreatorUuid) {
        return new CourseCreatorSkillDTO(u.uuid(), courseCreatorUuid, u.skillName(), u.skillUuid(), u.proficiencyLevel(), u.evidence(),
                u.lastAssessedOn(), u.verificationStatus(), u.verifiedAt(), u.verificationNotes(), u.createdDate(),
                u.createdBy(), u.updatedDate(), u.updatedBy());
    }
}
