package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.tenancy.dto.CreateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupMemberDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupRosterEntryDTO;
import apps.sarafrika.elimika.tenancy.dto.UpdateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.AcademicTier;
import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.factory.StudentGroupFactory;
import apps.sarafrika.elimika.tenancy.repository.AcademicTierRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupMemberRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.services.StudentGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentGroupServiceImpl implements StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final StudentGroupMemberRepository studentGroupMemberRepository;
    private final TrainingBranchRepository trainingBranchRepository;
    private final AcademicTierRepository academicTierRepository;

    @Override
    public StudentGroupDTO createGroup(UUID organisationUuid, CreateStudentGroupRequestDTO request) {
        StudentGroup group = StudentGroupFactory.toEntity(organisationUuid, request);
        validateStructure(group);
        StudentGroup saved = studentGroupRepository.save(group);
        return decorate(saved, 0L);
    }

    @Override
    public StudentGroupDTO updateGroup(UUID groupUuid, UpdateStudentGroupRequestDTO request) {
        StudentGroup group = findGroupOrThrow(groupUuid);
        StudentGroupFactory.applyUpdate(group, request);
        validateStructure(group);
        StudentGroup saved = studentGroupRepository.save(group);
        return decorate(saved, studentGroupMemberRepository.countByGroupUuid(groupUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGroupDTO> getGroupsForOrganisation(UUID organisationUuid, UUID branchUuid, UUID tierUuid) {
        List<StudentGroup> groups = findGroups(organisationUuid, branchUuid, tierUuid);
        if (groups.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : studentGroupMemberRepository.countByGroupUuids(
                groups.stream().map(StudentGroup::getUuid).toList())) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }

        Map<UUID, String> branchNames = branchNamesFor(groups);
        Map<UUID, AcademicTier> tiers = tiersFor(groups);

        return groups.stream()
                .map(g -> StudentGroupFactory.toDTO(
                        g,
                        counts.getOrDefault(g.getUuid(), 0L),
                        g.getBranchUuid() == null ? null : branchNames.get(g.getBranchUuid()),
                        g.getTierUuid() == null ? null : tiers.get(g.getTierUuid())))
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
                .map(StudentGroupFactory::toMemberDTO)
                .toList();
    }

    @Override
    public List<StudentGroupMemberDTO> addMembers(UUID groupUuid, List<UUID> studentUuids) {
        StudentGroup group = findGroupOrThrow(groupUuid);
        for (UUID studentUuid : studentUuids) {
            if (studentUuid != null
                    && !studentGroupMemberRepository.existsByGroupUuidAndStudentUuid(groupUuid, studentUuid)) {
                studentGroupMemberRepository.save(StudentGroupFactory.toMemberEntity(groupUuid, studentUuid));
            }
        }

        List<StudentGroupMemberDTO> members = getMembers(groupUuid);

        // Capacity is advisory, never a gate. Schools over-enrol as a matter of routine — a late
        // admission, a sibling placed together, a stream merged mid-term — and the office staff
        // doing it have no way to raise the cap in the moment. Rejecting the add would turn an
        // ordinary Tuesday into a support ticket while the student sits outside the system
        // entirely, which is strictly worse than a group that reads 41/40. The overflow is
        // reported instead: member_count and capacity both travel on StudentGroupDTO so the
        // frontend can flag it, and it is logged here so it is visible without opening the UI.
        Integer capacity = group.getCapacity();
        if (capacity != null && members.size() > capacity) {
            log.info("Student group {} is over its capacity: {} members against a capacity of {}",
                    groupUuid, members.size(), capacity);
        }

        return members;
    }

    @Override
    public void removeMember(UUID groupUuid, UUID studentUuid) {
        studentGroupMemberRepository.deleteByGroupUuidAndStudentUuid(groupUuid, studentUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentGroupRosterEntryDTO> getRoster(UUID organisationUuid,
                                                      UUID branchUuid,
                                                      UUID tierUuid,
                                                      UUID groupUuid,
                                                      Pageable pageable) {
        return studentGroupMemberRepository
                .findRoster(organisationUuid, branchUuid, tierUuid, groupUuid, pageable)
                // The projection carries the persisted storage key; clients need the public URL.
                .map(entry -> entry.withProfileImageUrl(FileUrlResolver.publicUrl(entry.profileImageUrl())));
    }

    /**
     * Rejects structure that points outside the group's own tenant.
     * <p>
     * No {@code @PreAuthorize} can express this: the caller genuinely may manage the organisation
     * named in the path, so the request passes authorization, yet the branch uuid in the body may
     * belong to a completely different tenant. Left unchecked, an org admin could attach their
     * cohort to a rival school's campus — and the group list would then leak that campus's name
     * back through {@code branch_name}. Ownership of the referenced rows is therefore a service
     * concern, checked against the group's own organisation.
     */
    private void validateStructure(StudentGroup group) {
        if (group.getBranchUuid() != null) {
            TrainingBranch branch = trainingBranchRepository.findByUuidAndDeletedFalse(group.getBranchUuid())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Training branch not found: " + group.getBranchUuid()));
            if (!Objects.equals(branch.getOrganisationUuid(), group.getOrganisationUuid())) {
                log.warn("Rejected student group pointing at branch {} owned by organisation {} from organisation {}",
                        branch.getUuid(), branch.getOrganisationUuid(), group.getOrganisationUuid());
                throw new IllegalArgumentException("Training branch does not belong to this organisation");
            }
        }

        if (group.getTierUuid() != null) {
            AcademicTier tier = academicTierRepository.findByUuid(group.getTierUuid())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Academic tier not found: " + group.getTierUuid()));
            // Platform tiers (null organisation) are shared; an organisation-defined tier belongs
            // to exactly one tenant and may not be borrowed by another.
            if (tier.getOrganisationUuid() != null
                    && !Objects.equals(tier.getOrganisationUuid(), group.getOrganisationUuid())) {
                log.warn("Rejected student group pointing at tier {} owned by organisation {} from organisation {}",
                        tier.getUuid(), tier.getOrganisationUuid(), group.getOrganisationUuid());
                throw new IllegalArgumentException("Academic tier does not belong to this organisation");
            }
        }
    }

    private List<StudentGroup> findGroups(UUID organisationUuid, UUID branchUuid, UUID tierUuid) {
        if (branchUuid != null && tierUuid != null) {
            return studentGroupRepository.findByOrganisationUuidAndBranchUuidAndTierUuidOrderByNameAsc(
                    organisationUuid, branchUuid, tierUuid);
        }
        if (branchUuid != null) {
            return studentGroupRepository.findByOrganisationUuidAndBranchUuidOrderByNameAsc(
                    organisationUuid, branchUuid);
        }
        if (tierUuid != null) {
            return studentGroupRepository.findByOrganisationUuidAndTierUuidOrderByNameAsc(organisationUuid, tierUuid);
        }
        return studentGroupRepository.findByOrganisationUuidOrderByNameAsc(organisationUuid);
    }

    /**
     * Branch names for a page of groups, in one round trip. Denormalising the name onto the DTO is
     * what lets the frontend label a filter pill without a second call per branch.
     */
    private Map<UUID, String> branchNamesFor(List<StudentGroup> groups) {
        Set<UUID> branchUuids = distinct(groups, StudentGroup::getBranchUuid);
        if (branchUuids.isEmpty()) {
            return Map.of();
        }
        return trainingBranchRepository.findByUuidIn(branchUuids).stream()
                .collect(Collectors.toMap(TrainingBranch::getUuid, TrainingBranch::getBranchName,
                        (first, second) -> first));
    }

    /** Academic tiers for a page of groups, in one round trip. See {@link #branchNamesFor(List)}. */
    private Map<UUID, AcademicTier> tiersFor(List<StudentGroup> groups) {
        Set<UUID> tierUuids = distinct(groups, StudentGroup::getTierUuid);
        if (tierUuids.isEmpty()) {
            return Map.of();
        }
        return academicTierRepository.findByUuidIn(tierUuids).stream()
                .collect(Collectors.toMap(AcademicTier::getUuid, Function.identity(), (first, second) -> first));
    }

    private Set<UUID> distinct(List<StudentGroup> groups, Function<StudentGroup, UUID> extractor) {
        return groups.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private StudentGroupDTO decorate(StudentGroup group, long memberCount) {
        String branchName = group.getBranchUuid() == null
                ? null
                : trainingBranchRepository.findByUuidAndDeletedFalse(group.getBranchUuid())
                        .map(TrainingBranch::getBranchName)
                        .orElse(null);
        AcademicTier tier = group.getTierUuid() == null
                ? null
                : academicTierRepository.findByUuid(group.getTierUuid()).orElse(null);
        return StudentGroupFactory.toDTO(group, memberCount, branchName, tier);
    }

    private StudentGroup findGroupOrThrow(UUID groupUuid) {
        return studentGroupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Student group not found: " + groupUuid));
    }
}
