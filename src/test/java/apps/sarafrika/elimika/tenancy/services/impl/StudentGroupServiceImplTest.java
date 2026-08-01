package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.CreateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupMemberDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupRosterEntryDTO;
import apps.sarafrika.elimika.tenancy.dto.UpdateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.AcademicTier;
import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import apps.sarafrika.elimika.tenancy.entity.StudentGroupMember;
import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.repository.AcademicTierRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupMemberRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentGroupServiceImplTest {

    @Mock
    private StudentGroupRepository studentGroupRepository;
    @Mock
    private StudentGroupMemberRepository studentGroupMemberRepository;
    @Mock
    private TrainingBranchRepository trainingBranchRepository;
    @Mock
    private AcademicTierRepository academicTierRepository;

    private StudentGroupServiceImpl service;

    private final UUID organisationUuid = UUID.randomUUID();
    private final UUID otherOrganisationUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentGroupServiceImpl(
                studentGroupRepository,
                studentGroupMemberRepository,
                trainingBranchRepository,
                academicTierRepository
        );
    }

    // ---------------------------------------------------------------- update semantics

    @Test
    void updateGroupReplacesEveryEditableFieldWithoutChangingIdentity() {
        // The uuid has to survive: class_marketplace_jobs stores group uuids with no foreign key,
        // so the delete-and-recreate workaround this endpoint replaces silently orphaned them.
        UUID branchUuid = UUID.randomUUID();
        UUID tierUuid = UUID.randomUUID();
        StudentGroup existing = group(UUID.randomUUID(), organisationUuid, "Grade 9 Stream A");
        existing.setDescription("old");
        existing.setGroupType("Stream A");
        existing.setCapacity(30);

        when(studentGroupRepository.findByUuid(existing.getUuid())).thenReturn(Optional.of(existing));
        stubBranch(branchUuid, organisationUuid, "Westlands Campus");
        stubTier(tierUuid, null, "Grade 9", 120);
        when(studentGroupMemberRepository.countByGroupUuid(existing.getUuid())).thenReturn(12L);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenAnswer(i -> i.getArgument(0));

        StudentGroupDTO updated = service.updateGroup(existing.getUuid(), new UpdateStudentGroupRequestDTO(
                "Grade 9 Blue", "renamed", "Blue", branchUuid, tierUuid, 45));

        assertThat(updated.uuid()).isEqualTo(existing.getUuid());
        assertThat(updated.organisationUuid()).isEqualTo(organisationUuid);
        assertThat(updated.name()).isEqualTo("Grade 9 Blue");
        assertThat(updated.description()).isEqualTo("renamed");
        assertThat(updated.groupType()).isEqualTo("Blue");
        assertThat(updated.branchUuid()).isEqualTo(branchUuid);
        assertThat(updated.branchName()).isEqualTo("Westlands Campus");
        assertThat(updated.tierUuid()).isEqualTo(tierUuid);
        assertThat(updated.tier()).isEqualTo("Grade 9");
        assertThat(updated.tierOrder()).isEqualTo(120);
        assertThat(updated.capacity()).isEqualTo(45);
        assertThat(updated.memberCount()).isEqualTo(12L);

        verify(studentGroupRepository, never()).delete(any());
    }

    @Test
    void updateGroupIsAFullReplaceSoOmittedFieldsAreCleared() {
        StudentGroup existing = group(UUID.randomUUID(), organisationUuid, "Grade 9 Stream A");
        existing.setDescription("was set");
        existing.setGroupType("Stream A");
        existing.setBranchUuid(UUID.randomUUID());
        existing.setTierUuid(UUID.randomUUID());
        existing.setCapacity(30);

        when(studentGroupRepository.findByUuid(existing.getUuid())).thenReturn(Optional.of(existing));
        when(studentGroupMemberRepository.countByGroupUuid(existing.getUuid())).thenReturn(0L);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenAnswer(i -> i.getArgument(0));

        StudentGroupDTO updated = service.updateGroup(existing.getUuid(),
                new UpdateStudentGroupRequestDTO("Grade 9", null, null, null, null, null));

        assertThat(updated.description()).isNull();
        assertThat(updated.groupType()).isNull();
        assertThat(updated.branchUuid()).isNull();
        assertThat(updated.branchName()).isNull();
        assertThat(updated.tierUuid()).isNull();
        assertThat(updated.tier()).isNull();
        assertThat(updated.tierOrder()).isNull();
        assertThat(updated.capacity()).isNull();
    }

    @Test
    void updateGroupRejectsAnUnknownGroup() {
        UUID missing = UUID.randomUUID();
        when(studentGroupRepository.findByUuid(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateGroup(missing,
                new UpdateStudentGroupRequestDTO("Grade 9", null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student group not found");

        verify(studentGroupRepository, never()).save(any());
    }

    // ---------------------------------------------------------- cross-tenant branch rejection

    @Test
    void createGroupRejectsABranchOwnedByAnotherOrganisation() {
        // No @PreAuthorize can catch this: the caller really does manage the organisation in the
        // path, and only the body reveals that the branch belongs to a different tenant.
        UUID foreignBranchUuid = UUID.randomUUID();
        stubBranch(foreignBranchUuid, otherOrganisationUuid, "Rival Campus");

        assertThatThrownBy(() -> service.createGroup(organisationUuid, new CreateStudentGroupRequestDTO(
                "Grade 9 Blue", null, "Blue", foreignBranchUuid, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Training branch does not belong to this organisation");

        verify(studentGroupRepository, never()).save(any());
    }

    @Test
    void updateGroupRejectsRepointingAtAnotherOrganisationsBranch() {
        UUID foreignBranchUuid = UUID.randomUUID();
        StudentGroup existing = group(UUID.randomUUID(), organisationUuid, "Grade 9 Stream A");

        when(studentGroupRepository.findByUuid(existing.getUuid())).thenReturn(Optional.of(existing));
        stubBranch(foreignBranchUuid, otherOrganisationUuid, "Rival Campus");

        assertThatThrownBy(() -> service.updateGroup(existing.getUuid(), new UpdateStudentGroupRequestDTO(
                "Grade 9 Blue", null, "Blue", foreignBranchUuid, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Training branch does not belong to this organisation");

        verify(studentGroupRepository, never()).save(any());
    }

    @Test
    void createGroupAcceptsABranchInItsOwnOrganisation() {
        UUID branchUuid = UUID.randomUUID();
        stubBranch(branchUuid, organisationUuid, "Westlands Campus");
        when(studentGroupRepository.save(any(StudentGroup.class))).thenAnswer(i -> i.getArgument(0));

        StudentGroupDTO created = service.createGroup(organisationUuid, new CreateStudentGroupRequestDTO(
                "Grade 9 Blue", null, "Blue", branchUuid, null, 40));

        assertThat(created.branchUuid()).isEqualTo(branchUuid);
        assertThat(created.branchName()).isEqualTo("Westlands Campus");
        assertThat(created.capacity()).isEqualTo(40);
        assertThat(created.memberCount()).isZero();
    }

    @Test
    void createGroupRejectsABranchThatDoesNotExist() {
        UUID branchUuid = UUID.randomUUID();
        when(trainingBranchRepository.findByUuidAndDeletedFalse(branchUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createGroup(organisationUuid, new CreateStudentGroupRequestDTO(
                "Grade 9 Blue", null, "Blue", branchUuid, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Training branch not found");

        verify(studentGroupRepository, never()).save(any());
    }

    @Test
    void createGroupRejectsATierPrivateToAnotherOrganisation() {
        UUID tierUuid = UUID.randomUUID();
        stubTier(tierUuid, otherOrganisationUuid, "Rival Grade 9", 120);

        assertThatThrownBy(() -> service.createGroup(organisationUuid, new CreateStudentGroupRequestDTO(
                "Grade 9 Blue", null, "Blue", null, tierUuid, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Academic tier does not belong to this organisation");

        verify(studentGroupRepository, never()).save(any());
    }

    @Test
    void createGroupAcceptsAPlatformTierBecauseTheCatalogueIsShared() {
        UUID tierUuid = UUID.randomUUID();
        stubTier(tierUuid, null, "Grade 9", 120);
        when(studentGroupRepository.save(any(StudentGroup.class))).thenAnswer(i -> i.getArgument(0));

        StudentGroupDTO created = service.createGroup(organisationUuid, new CreateStudentGroupRequestDTO(
                "Grade 9 Blue", null, "Blue", null, tierUuid, null));

        assertThat(created.tierUuid()).isEqualTo(tierUuid);
        assertThat(created.tier()).isEqualTo("Grade 9");
        assertThat(created.tierOrder()).isEqualTo(120);
    }

    @Test
    void createGroupSkipsStructureChecksWhenTheGroupIsUnassigned() {
        when(studentGroupRepository.save(any(StudentGroup.class))).thenAnswer(i -> i.getArgument(0));

        StudentGroupDTO created = service.createGroup(organisationUuid,
                new CreateStudentGroupRequestDTO("Legacy cohort", null, null, null, null, null));

        assertThat(created.branchUuid()).isNull();
        assertThat(created.tierUuid()).isNull();
        verify(trainingBranchRepository, never()).findByUuidAndDeletedFalse(any());
        verify(academicTierRepository, never()).findByUuid(any());
    }

    // ------------------------------------------------------------ capacity is reported, not enforced

    @Test
    void addMembersEnrolsPastCapacityRatherThanRejecting() {
        // Schools over-enrol routinely and the office staff doing it cannot raise the cap in the
        // moment; blocking the add would leave the student outside the system entirely.
        UUID groupUuid = UUID.randomUUID();
        StudentGroup full = group(groupUuid, organisationUuid, "Grade 9 Blue");
        full.setCapacity(2);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID overflow = UUID.randomUUID();

        when(studentGroupRepository.findByUuid(groupUuid)).thenReturn(Optional.of(full));
        when(studentGroupMemberRepository.existsByGroupUuidAndStudentUuid(any(), any())).thenReturn(false);
        when(studentGroupMemberRepository.findByGroupUuid(groupUuid))
                .thenReturn(List.of(member(groupUuid, first), member(groupUuid, second), member(groupUuid, overflow)));

        List<StudentGroupMemberDTO> members = service.addMembers(groupUuid, List.of(first, second, overflow));

        assertThat(members).hasSize(3);
        verify(studentGroupMemberRepository, org.mockito.Mockito.times(3)).save(any(StudentGroupMember.class));
    }

    @Test
    void addMembersReportsOverflowThroughTheGroupDtoRatherThanAnError() {
        UUID groupUuid = UUID.randomUUID();
        StudentGroup full = group(groupUuid, organisationUuid, "Grade 9 Blue");
        full.setCapacity(2);

        when(studentGroupRepository.findByOrganisationUuidOrderByNameAsc(organisationUuid))
                .thenReturn(List.of(full));
        when(studentGroupMemberRepository.countByGroupUuids(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{groupUuid, 3L}));

        StudentGroupDTO dto = service.getGroupsForOrganisation(organisationUuid, null, null).getFirst();

        // Both numbers travel together so the frontend can flag 3/2 without a second call.
        assertThat(dto.capacity()).isEqualTo(2);
        assertThat(dto.memberCount()).isEqualTo(3L);
    }

    @Test
    void addMembersSkipsStudentsAlreadyInTheGroup() {
        UUID groupUuid = UUID.randomUUID();
        UUID existingStudent = UUID.randomUUID();
        UUID newStudent = UUID.randomUUID();

        when(studentGroupRepository.findByUuid(groupUuid))
                .thenReturn(Optional.of(group(groupUuid, organisationUuid, "Grade 9 Blue")));
        when(studentGroupMemberRepository.existsByGroupUuidAndStudentUuid(groupUuid, existingStudent)).thenReturn(true);
        when(studentGroupMemberRepository.existsByGroupUuidAndStudentUuid(groupUuid, newStudent)).thenReturn(false);
        when(studentGroupMemberRepository.findByGroupUuid(groupUuid))
                .thenReturn(List.of(member(groupUuid, existingStudent), member(groupUuid, newStudent)));

        service.addMembers(groupUuid, List.of(existingStudent, newStudent));

        ArgumentCaptor<StudentGroupMember> captor = ArgumentCaptor.forClass(StudentGroupMember.class);
        verify(studentGroupMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getStudentUuid()).isEqualTo(newStudent);
    }

    // --------------------------------------------------------------- branch / tier filtering

    @Test
    void listingWithoutFiltersReadsEveryGroupInTheOrganisation() {
        when(studentGroupRepository.findByOrganisationUuidOrderByNameAsc(organisationUuid)).thenReturn(List.of());

        assertThat(service.getGroupsForOrganisation(organisationUuid, null, null)).isEmpty();

        verify(studentGroupRepository).findByOrganisationUuidOrderByNameAsc(organisationUuid);
    }

    @Test
    void listingByBranchNarrowsToThatBranch() {
        UUID branchUuid = UUID.randomUUID();
        when(studentGroupRepository.findByOrganisationUuidAndBranchUuidOrderByNameAsc(organisationUuid, branchUuid))
                .thenReturn(List.of());

        assertThat(service.getGroupsForOrganisation(organisationUuid, branchUuid, null)).isEmpty();

        verify(studentGroupRepository)
                .findByOrganisationUuidAndBranchUuidOrderByNameAsc(organisationUuid, branchUuid);
        verify(studentGroupRepository, never()).findByOrganisationUuidOrderByNameAsc(any());
    }

    @Test
    void listingByTierNarrowsToThatTier() {
        UUID tierUuid = UUID.randomUUID();
        when(studentGroupRepository.findByOrganisationUuidAndTierUuidOrderByNameAsc(organisationUuid, tierUuid))
                .thenReturn(List.of());

        assertThat(service.getGroupsForOrganisation(organisationUuid, null, tierUuid)).isEmpty();

        verify(studentGroupRepository).findByOrganisationUuidAndTierUuidOrderByNameAsc(organisationUuid, tierUuid);
        verify(studentGroupRepository, never()).findByOrganisationUuidOrderByNameAsc(any());
    }

    @Test
    void listingByBranchAndTierAppliesBothFiltersInTheQuery() {
        UUID branchUuid = UUID.randomUUID();
        UUID tierUuid = UUID.randomUUID();
        when(studentGroupRepository.findByOrganisationUuidAndBranchUuidAndTierUuidOrderByNameAsc(
                organisationUuid, branchUuid, tierUuid)).thenReturn(List.of());

        assertThat(service.getGroupsForOrganisation(organisationUuid, branchUuid, tierUuid)).isEmpty();

        verify(studentGroupRepository).findByOrganisationUuidAndBranchUuidAndTierUuidOrderByNameAsc(
                organisationUuid, branchUuid, tierUuid);
        verify(studentGroupRepository, never())
                .findByOrganisationUuidAndBranchUuidOrderByNameAsc(any(), any());
        verify(studentGroupRepository, never())
                .findByOrganisationUuidAndTierUuidOrderByNameAsc(any(), any());
    }

    @Test
    void listingLabelsGroupsWithBranchAndTierInOneRoundTripEach() {
        // Denormalising the names is what keeps the Groups page at three requests: without them
        // every filter pill would need a lookup of its own.
        UUID branchUuid = UUID.randomUUID();
        UUID tierUuid = UUID.randomUUID();
        StudentGroup blue = group(UUID.randomUUID(), organisationUuid, "Grade 9 Blue");
        blue.setBranchUuid(branchUuid);
        blue.setTierUuid(tierUuid);
        StudentGroup red = group(UUID.randomUUID(), organisationUuid, "Grade 9 Red");
        red.setBranchUuid(branchUuid);
        red.setTierUuid(tierUuid);
        StudentGroup legacy = group(UUID.randomUUID(), organisationUuid, "Old cohort");

        when(studentGroupRepository.findByOrganisationUuidOrderByNameAsc(organisationUuid))
                .thenReturn(List.of(blue, red, legacy));
        when(studentGroupMemberRepository.countByGroupUuids(anyList())).thenReturn(List.of());
        when(trainingBranchRepository.findByUuidIn(any()))
                .thenReturn(List.of(branch(branchUuid, organisationUuid, "Westlands Campus")));
        when(academicTierRepository.findByUuidIn(any()))
                .thenReturn(List.of(tier(tierUuid, null, "Grade 9", 120)));

        List<StudentGroupDTO> groups = service.getGroupsForOrganisation(organisationUuid, null, null);

        assertThat(groups).extracting(StudentGroupDTO::branchName)
                .containsExactly("Westlands Campus", "Westlands Campus", null);
        assertThat(groups).extracting(StudentGroupDTO::tier)
                .containsExactly("Grade 9", "Grade 9", null);
        assertThat(groups).extracting(StudentGroupDTO::tierOrder)
                .containsExactly(120, 120, null);

        // One lookup for both groups sharing the branch, not one per group.
        verify(trainingBranchRepository).findByUuidIn(any());
        verify(academicTierRepository).findByUuidIn(any());
    }

    // ------------------------------------------------------------------------- roster paging

    @Test
    void rosterPagingPassesEveryFilterAndThePageableStraightToTheQuery() {
        UUID branchUuid = UUID.randomUUID();
        UUID tierUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        Pageable pageable = PageRequest.of(2, 25);
        StudentGroupRosterEntryDTO entry = rosterEntry(groupUuid, null);

        when(studentGroupMemberRepository.findRoster(organisationUuid, branchUuid, tierUuid, groupUuid, pageable))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 51));

        Page<StudentGroupRosterEntryDTO> page =
                service.getRoster(organisationUuid, branchUuid, tierUuid, groupUuid, pageable);

        assertThat(page.getTotalElements()).isEqualTo(51);
        assertThat(page.getNumber()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(25);
        assertThat(page.getContent()).hasSize(1);

        // Paging must stay in the database: the page the caller asked for is the page that is read.
        verify(studentGroupMemberRepository)
                .findRoster(organisationUuid, branchUuid, tierUuid, groupUuid, pageable);
    }

    @Test
    void rosterPagingLeavesNullFiltersUnrestricted() {
        Pageable pageable = PageRequest.of(0, 20);
        when(studentGroupMemberRepository.findRoster(organisationUuid, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.getRoster(organisationUuid, null, null, null, pageable)).isEmpty();

        verify(studentGroupMemberRepository).findRoster(organisationUuid, null, null, null, pageable);
    }

    @Test
    void rosterResolvesThePersistedStorageKeyIntoAPublicUrl() {
        Pageable pageable = PageRequest.of(0, 20);
        UUID groupUuid = UUID.randomUUID();
        StudentGroupRosterEntryDTO entry = rosterEntry(groupUuid, "profile_images/abc.jpg");

        when(studentGroupMemberRepository.findRoster(organisationUuid, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(entry), pageable, 1));

        StudentGroupRosterEntryDTO resolved =
                service.getRoster(organisationUuid, null, null, null, pageable).getContent().getFirst();

        assertThat(resolved.profileImageUrl()).startsWith("/api/v1/files/");
        // Everything else on the row must survive the rewrite untouched.
        assertThat(resolved.studentUuid()).isEqualTo(entry.studentUuid());
        assertThat(resolved.groupUuid()).isEqualTo(groupUuid);
        assertThat(resolved.fullName()).isEqualTo("Asha Kimani");
        assertThat(resolved.streamLabel()).isEqualTo("Blue");
        assertThat(resolved.tier()).isEqualTo("Grade 9");
        assertThat(resolved.dob()).isEqualTo(LocalDate.of(2011, 3, 4));
    }

    // ------------------------------------------------------------------------------- helpers

    private void stubBranch(UUID branchUuid, UUID owningOrganisation, String name) {
        lenient().when(trainingBranchRepository.findByUuidAndDeletedFalse(branchUuid))
                .thenReturn(Optional.of(branch(branchUuid, owningOrganisation, name)));
    }

    private void stubTier(UUID tierUuid, UUID owningOrganisation, String name, int order) {
        lenient().when(academicTierRepository.findByUuid(tierUuid))
                .thenReturn(Optional.of(tier(tierUuid, owningOrganisation, name, order)));
    }

    private StudentGroup group(UUID uuid, UUID organisation, String name) {
        StudentGroup group = new StudentGroup();
        group.setUuid(uuid);
        group.setOrganisationUuid(organisation);
        group.setName(name);
        return group;
    }

    private TrainingBranch branch(UUID uuid, UUID organisation, String name) {
        TrainingBranch branch = new TrainingBranch();
        branch.setUuid(uuid);
        branch.setOrganisationUuid(organisation);
        branch.setBranchName(name);
        return branch;
    }

    private AcademicTier tier(UUID uuid, UUID organisation, String name, int order) {
        AcademicTier tier = new AcademicTier();
        tier.setUuid(uuid);
        tier.setOrganisationUuid(organisation);
        tier.setName(name);
        tier.setTierOrder(order);
        tier.setEducationSystem("KE");
        tier.setActive(true);
        return tier;
    }

    private StudentGroupMember member(UUID groupUuid, UUID studentUuid) {
        StudentGroupMember member = new StudentGroupMember();
        member.setUuid(UUID.randomUUID());
        member.setGroupUuid(groupUuid);
        member.setStudentUuid(studentUuid);
        return member;
    }

    private StudentGroupRosterEntryDTO rosterEntry(UUID groupUuid, String profileImageUrl) {
        return new StudentGroupRosterEntryDTO(
                UUID.randomUUID(),
                groupUuid,
                "Grade 9 Blue",
                "Grade 9",
                "Blue",
                "Asha Kimani",
                "asha@example.test",
                "+254700000000",
                LocalDate.of(2011, 3, 4),
                profileImageUrl,
                LocalDateTime.of(2026, 1, 12, 9, 0));
    }
}
