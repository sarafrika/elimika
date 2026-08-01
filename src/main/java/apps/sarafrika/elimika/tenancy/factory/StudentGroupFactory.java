package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.CreateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupMemberDTO;
import apps.sarafrika.elimika.tenancy.dto.UpdateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.AcademicTier;
import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import apps.sarafrika.elimika.tenancy.entity.StudentGroupMember;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Assembly of student groups and their membership rows.
 * <p>
 * Replaces the Lombok builders these entities used to carry: the repository convention is
 * constructors and factories so every field a group is created with is visible in one place.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StudentGroupFactory {

    public static StudentGroup toEntity(UUID organisationUuid, CreateStudentGroupRequestDTO request) {
        StudentGroup group = new StudentGroup();
        group.setOrganisationUuid(organisationUuid);
        group.setName(request.name());
        group.setDescription(request.description());
        group.setGroupType(request.groupType());
        group.setBranchUuid(request.branchUuid());
        group.setTierUuid(request.tierUuid());
        group.setCapacity(request.capacity());
        return group;
    }

    /**
     * Applies a full replacement onto an existing group. The organisation is untouched: a group
     * cannot change tenants.
     */
    public static void applyUpdate(StudentGroup group, UpdateStudentGroupRequestDTO request) {
        group.setName(request.name());
        group.setDescription(request.description());
        group.setGroupType(request.groupType());
        group.setBranchUuid(request.branchUuid());
        group.setTierUuid(request.tierUuid());
        group.setCapacity(request.capacity());
    }

    /**
     * @param branchName resolved branch name, or {@code null} when the group is unassigned
     * @param tier       resolved academic tier, or {@code null} when the group is unassigned
     */
    public static StudentGroupDTO toDTO(StudentGroup group, long memberCount, String branchName, AcademicTier tier) {
        return new StudentGroupDTO(
                group.getUuid(),
                group.getOrganisationUuid(),
                group.getName(),
                group.getDescription(),
                group.getGroupType(),
                group.getBranchUuid(),
                branchName,
                group.getTierUuid(),
                tier == null ? null : tier.getName(),
                tier == null ? null : tier.getTierOrder(),
                group.getCapacity(),
                memberCount,
                group.getCreatedDate()
        );
    }

    public static StudentGroupMember toMemberEntity(UUID groupUuid, UUID studentUuid) {
        StudentGroupMember member = new StudentGroupMember();
        member.setGroupUuid(groupUuid);
        member.setStudentUuid(studentUuid);
        return member;
    }

    public static StudentGroupMemberDTO toMemberDTO(StudentGroupMember member) {
        return new StudentGroupMemberDTO(
                member.getUuid(),
                member.getGroupUuid(),
                member.getStudentUuid(),
                member.getCreatedDate()
        );
    }
}
