package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorEducationDTO;
import apps.sarafrika.elimika.instructor.internal.InstructorProfileBridge;
import apps.sarafrika.elimika.instructor.service.InstructorEducationService;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.UserEducationDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Instructor education, kept on the user-owned professional profile. */
@Service
@RequiredArgsConstructor
@Transactional
public class InstructorEducationServiceImpl implements InstructorEducationService {

    private static final String NOT_FOUND = "Instructor education with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final InstructorProfileBridge bridge;

    @Override
    public InstructorEducationDTO createInstructorEducation(InstructorEducationDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.instructorUuid());
        return toDto(profileService.education().create(userUuid, toUser(dto)), dto.instructorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorEducationDTO getInstructorEducationByUuid(UUID uuid) {
        UserEducationDTO education = require(uuid);
        return toDto(education, bridge.findInstructorUuid(education.userUuid()).orElse(null));
    }

    @Override
    public InstructorEducationDTO updateInstructorEducation(UUID uuid, InstructorEducationDTO dto) {
        UserEducationDTO existing = require(uuid);
        UserEducationDTO saved = profileService.education()
                .update(existing.userUuid(), uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, bridge.findInstructorUuid(saved.userUuid()).orElse(dto.instructorUuid()));
    }

    @Override
    public void deleteInstructorEducation(UUID uuid) {
        profileService.education().delete(require(uuid).userUuid(), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorEducationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        InstructorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserEducationDTO> page = profileService.education().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> instructors = bridge.instructorUuidsByUser(page.map(UserEducationDTO::userUuid).toSet());
        return page.map(education -> toDto(education, instructors.get(education.userUuid())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorEducationDTO> getEducationByInstructorUuid(UUID instructorUuid) {
        return bridge.findUserUuid(instructorUuid)
                .map(userUuid -> profileService.education().list(userUuid).stream()
                        .map(education -> toDto(education, instructorUuid)).toList())
                .orElse(List.of());
    }

    private UserEducationDTO require(UUID uuid) {
        return profileService.education().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    private static UserEducationDTO toUser(InstructorEducationDTO dto) {
        return new UserEducationDTO(null, null, dto.qualification(), dto.fieldOfStudy(), dto.schoolName(),
                dto.startYear(), dto.yearCompleted(), dto.certificateNumber(), null, null, null, null);
    }

    private static InstructorEducationDTO toDto(UserEducationDTO e, UUID instructorUuid) {
        return new InstructorEducationDTO(e.uuid(), instructorUuid, e.qualification(), e.fieldOfStudy(), e.schoolName(),
                e.startYear(), e.yearCompleted(), e.certificateNumber(), e.createdDate(), e.createdBy(),
                e.updatedDate(), e.updatedBy());
    }
}
