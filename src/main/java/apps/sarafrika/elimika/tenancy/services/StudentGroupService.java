package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.CreateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupMemberDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupRosterEntryDTO;
import apps.sarafrika.elimika.tenancy.dto.UpdateStudentGroupRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Organisation student groups (cohorts / streams) and their membership.
 */
public interface StudentGroupService {

    StudentGroupDTO createGroup(UUID organisationUuid, CreateStudentGroupRequestDTO request);

    /**
     * Replaces a group's editable attributes. Exists so a group can be corrected in place:
     * delete-and-recreate changes the group uuid, and {@code class_marketplace_jobs} stores group
     * uuids with no foreign key, so recreating silently orphans those references.
     */
    StudentGroupDTO updateGroup(UUID groupUuid, UpdateStudentGroupRequestDTO request);

    /**
     * Groups for an organisation, optionally narrowed to one branch and/or one academic tier.
     * Null filters mean "no restriction".
     */
    List<StudentGroupDTO> getGroupsForOrganisation(UUID organisationUuid, UUID branchUuid, UUID tierUuid);

    void deleteGroup(UUID groupUuid);

    List<StudentGroupMemberDTO> getMembers(UUID groupUuid);

    List<StudentGroupMemberDTO> addMembers(UUID groupUuid, List<UUID> studentUuids);

    void removeMember(UUID groupUuid, UUID studentUuid);

    /**
     * The organisation's student roster: one paginated table of students with the group they sit
     * in, optionally narrowed by branch, tier or a single group.
     */
    Page<StudentGroupRosterEntryDTO> getRoster(UUID organisationUuid,
                                               UUID branchUuid,
                                               UUID tierUuid,
                                               UUID groupUuid,
                                               Pageable pageable);
}
