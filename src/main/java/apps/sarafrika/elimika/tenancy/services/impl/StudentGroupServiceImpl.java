package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.CreateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupMemberDTO;
import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import apps.sarafrika.elimika.tenancy.entity.StudentGroupMember;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupMemberRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupRepository;
import apps.sarafrika.elimika.tenancy.services.StudentGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentGroupServiceImpl implements StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final StudentGroupMemberRepository studentGroupMemberRepository;

    @Override
    public StudentGroupDTO createGroup(UUID organisationUuid, CreateStudentGroupRequestDTO request) {
        StudentGroup group = StudentGroup.builder()
                .organisationUuid(organisationUuid)
                .name(request.name())
                .description(request.description())
                .groupType(request.groupType())
                .build();
        StudentGroup saved = studentGroupRepository.save(group);
        return toDTO(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGroupDTO> getGroupsForOrganisation(UUID organisationUuid) {
        List<StudentGroup> groups = studentGroupRepository.findByOrganisationUuidOrderByNameAsc(organisationUuid);
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : studentGroupMemberRepository.countByGroupUuids(groups.stream().map(StudentGroup::getUuid).toList())) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return groups.stream()
                .map(g -> toDTO(g, counts.getOrDefault(g.getUuid(), 0L)))
                .toList();
    }

    @Override
    public void deleteGroup(UUID groupUuid) {
        StudentGroup group = findGroupOrThrow(groupUuid);
        studentGroupRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGroupMemberDTO> getMembers(UUID groupUuid) {
        return studentGroupMemberRepository.findByGroupUuid(groupUuid).stream()
                .map(this::toMemberDTO)
                .toList();
    }

    @Override
    public List<StudentGroupMemberDTO> addMembers(UUID groupUuid, List<UUID> studentUuids) {
        findGroupOrThrow(groupUuid);
        for (UUID studentUuid : studentUuids) {
            if (!studentGroupMemberRepository.existsByGroupUuidAndStudentUuid(groupUuid, studentUuid)) {
                studentGroupMemberRepository.save(StudentGroupMember.builder()
                        .groupUuid(groupUuid)
                        .studentUuid(studentUuid)
                        .build());
            }
        }
        return getMembers(groupUuid);
    }

    @Override
    public void removeMember(UUID groupUuid, UUID studentUuid) {
        studentGroupMemberRepository.deleteByGroupUuidAndStudentUuid(groupUuid, studentUuid);
    }

    private StudentGroup findGroupOrThrow(UUID groupUuid) {
        return studentGroupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Student group not found: " + groupUuid));
    }

    private StudentGroupDTO toDTO(StudentGroup g, long memberCount) {
        return new StudentGroupDTO(
                g.getUuid(),
                g.getOrganisationUuid(),
                g.getName(),
                g.getDescription(),
                g.getGroupType(),
                memberCount,
                g.getCreatedDate()
        );
    }

    private StudentGroupMemberDTO toMemberDTO(StudentGroupMember m) {
        return new StudentGroupMemberDTO(m.getUuid(), m.getGroupUuid(), m.getStudentUuid(), m.getCreatedDate());
    }
}
