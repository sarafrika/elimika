package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorSkillDTO;
import apps.sarafrika.elimika.instructor.internal.InstructorProfileBridge;
import apps.sarafrika.elimika.instructor.service.InstructorSkillService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Instructor skills, kept in the user-owned skills wallet so every domain of the user shares them. */
@Service
@RequiredArgsConstructor
@Transactional
public class InstructorSkillServiceImpl implements InstructorSkillService {

    private static final String NOT_FOUND = "Instructor skill with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final InstructorProfileBridge bridge;

    @Override
    public InstructorSkillDTO createInstructorSkill(InstructorSkillDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.instructorUuid());
        ProficiencyLevel level = dto.proficiencyLevel() == null ? ProficiencyLevel.BEGINNER : dto.proficiencyLevel();
        UserSkillDTO saved = profileService.skills().create(userUuid,
                UserSkillDTO.claim(dto.skillName(), level, null, null));
        return toDto(saved, dto.instructorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorSkillDTO getInstructorSkillByUuid(UUID uuid) {
        UserSkillDTO skill = profileService.skills().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
        return toDto(skill, bridge.findInstructorUuid(skill.userUuid()).orElse(null));
    }

    @Override
    public InstructorSkillDTO updateInstructorSkill(UUID uuid, InstructorSkillDTO dto) {
        UserSkillDTO existing = profileService.skills().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
        UserSkillDTO changes = UserSkillDTO.claim(dto.skillName(), dto.proficiencyLevel(), null, null);
        UserSkillDTO saved = profileService.skills().update(existing.userUuid(), uuid, ProfileRecords.overlay(changes, existing));
        return toDto(saved, bridge.findInstructorUuid(saved.userUuid()).orElse(dto.instructorUuid()));
    }

    @Override
    public void deleteInstructorSkill(UUID uuid) {
        UserSkillDTO existing = profileService.skills().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
        profileService.skills().delete(existing.userUuid(), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorSkillDTO> search(Map<String, String> searchParams, Pageable pageable) {
        InstructorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserSkillDTO> page = profileService.skills().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> instructors = bridge.instructorUuidsByUser(page.map(UserSkillDTO::userUuid).toSet());
        return page.map(skill -> toDto(skill, instructors.get(skill.userUuid())));
    }

    static InstructorSkillDTO toDto(UserSkillDTO skill, UUID instructorUuid) {
        return new InstructorSkillDTO(skill.uuid(), instructorUuid, skill.skillName(), skill.skillUuid(),
                skill.proficiencyLevel(), skill.createdDate(), skill.createdBy(), skill.updatedDate(), skill.updatedBy());
    }
}
