package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorExperienceDTO;
import apps.sarafrika.elimika.instructor.internal.InstructorProfileBridge;
import apps.sarafrika.elimika.instructor.service.InstructorExperienceService;
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

/** Instructor experience, kept on the user-owned professional profile. */
@Service
@RequiredArgsConstructor
@Transactional
public class InstructorExperienceServiceImpl implements InstructorExperienceService {

    private static final String NOT_FOUND = "Instructor experience with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final InstructorProfileBridge bridge;

    @Override
    public InstructorExperienceDTO createInstructorExperience(InstructorExperienceDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.instructorUuid());
        return toDto(profileService.experience().create(userUuid, toUser(dto)), dto.instructorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorExperienceDTO getInstructorExperienceByUuid(UUID uuid) {
        UserExperienceDTO experience = require(uuid);
        return toDto(experience, bridge.findInstructorUuid(experience.userUuid()).orElse(null));
    }

    @Override
    public InstructorExperienceDTO updateInstructorExperience(UUID uuid, InstructorExperienceDTO dto) {
        UserExperienceDTO existing = require(uuid);
        UserExperienceDTO saved = profileService.experience()
                .update(existing.userUuid(), uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, bridge.findInstructorUuid(saved.userUuid()).orElse(dto.instructorUuid()));
    }

    @Override
    public void deleteInstructorExperience(UUID uuid) {
        profileService.experience().delete(require(uuid).userUuid(), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorExperienceDTO> search(Map<String, String> searchParams, Pageable pageable) {
        InstructorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserExperienceDTO> page = profileService.experience().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> instructors = bridge.instructorUuidsByUser(page.map(UserExperienceDTO::userUuid).toSet());
        return page.map(experience -> toDto(experience, instructors.get(experience.userUuid())));
    }

    private UserExperienceDTO require(UUID uuid) {
        return profileService.experience().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    private static UserExperienceDTO toUser(InstructorExperienceDTO dto) {
        return new UserExperienceDTO(null, null, dto.position(), dto.organizationName(), dto.responsibilities(),
                dto.yearsOfExperience(), dto.startDate(), dto.endDate(), dto.isCurrentPosition(), null,
                null, null, null, null);
    }

    private static InstructorExperienceDTO toDto(UserExperienceDTO e, UUID instructorUuid) {
        return new InstructorExperienceDTO(e.uuid(), instructorUuid, e.position(), e.organizationName(),
                e.responsibilities(), e.yearsOfExperience(), e.startDate(), e.endDate(), e.isCurrentPosition(),
                e.createdDate(), e.createdBy(), e.updatedDate(), e.updatedBy());
    }
}
