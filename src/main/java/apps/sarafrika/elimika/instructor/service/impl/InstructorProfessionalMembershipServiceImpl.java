package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.dto.InstructorProfessionalMembershipDTO;
import apps.sarafrika.elimika.instructor.internal.InstructorProfileBridge;
import apps.sarafrika.elimika.instructor.service.InstructorProfessionalMembershipService;
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

/** Instructor professional memberships, kept on the user-owned professional profile. */
@Service
@RequiredArgsConstructor
@Transactional
public class InstructorProfessionalMembershipServiceImpl implements InstructorProfessionalMembershipService {

    private static final String NOT_FOUND = "Instructor professional membership with ID %s not found";

    private final ProfessionalProfileService profileService;
    private final InstructorProfileBridge bridge;

    @Override
    public InstructorProfessionalMembershipDTO createInstructorProfessionalMembership(InstructorProfessionalMembershipDTO dto) {
        UUID userUuid = bridge.requireUserUuid(dto.instructorUuid());
        return toDto(profileService.memberships().create(userUuid, toUser(dto)), dto.instructorUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorProfessionalMembershipDTO getInstructorProfessionalMembershipByUuid(UUID uuid) {
        UserMembershipDTO membership = require(uuid);
        return toDto(membership, bridge.findInstructorUuid(membership.userUuid()).orElse(null));
    }

    @Override
    public InstructorProfessionalMembershipDTO updateInstructorProfessionalMembership(UUID uuid,
                                                                                      InstructorProfessionalMembershipDTO dto) {
        UserMembershipDTO existing = require(uuid);
        UserMembershipDTO saved = profileService.memberships()
                .update(existing.userUuid(), uuid, ProfileRecords.overlay(toUser(dto), existing));
        return toDto(saved, bridge.findInstructorUuid(saved.userUuid()).orElse(dto.instructorUuid()));
    }

    @Override
    public void deleteInstructorProfessionalMembership(UUID uuid) {
        profileService.memberships().delete(require(uuid).userUuid(), uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorProfessionalMembershipDTO> search(Map<String, String> searchParams, Pageable pageable) {
        InstructorProfileBridge.ScopedSearch scoped = bridge.scope(searchParams);
        Page<UserMembershipDTO> page = profileService.memberships().search(scoped.params(), scoped.userUuids(), pageable);
        Map<UUID, UUID> instructors = bridge.instructorUuidsByUser(page.map(UserMembershipDTO::userUuid).toSet());
        return page.map(membership -> toDto(membership, instructors.get(membership.userUuid())));
    }

    private UserMembershipDTO require(UUID uuid) {
        return profileService.memberships().find(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NOT_FOUND, uuid)));
    }

    private static UserMembershipDTO toUser(InstructorProfessionalMembershipDTO dto) {
        return new UserMembershipDTO(null, null, dto.organizationName(), dto.membershipNumber(), dto.startDate(),
                dto.endDate(), dto.isActive(), null, null, null, null);
    }

    private static InstructorProfessionalMembershipDTO toDto(UserMembershipDTO m, UUID instructorUuid) {
        return new InstructorProfessionalMembershipDTO(m.uuid(), instructorUuid, m.organizationName(),
                m.membershipNumber(), m.startDate(), m.endDate(), m.isActive(), m.createdDate(), m.createdBy(),
                m.updatedDate(), m.updatedBy());
    }
}
