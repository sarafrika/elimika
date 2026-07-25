package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.CreateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupMemberDTO;

import java.util.List;
import java.util.UUID;

/**
 * Organisation student groups (cohorts / streams) and their membership.
 */
public interface StudentGroupService {

    StudentGroupDTO createGroup(UUID organisationUuid, CreateStudentGroupRequestDTO request);

    List<StudentGroupDTO> getGroupsForOrganisation(UUID organisationUuid);

    void deleteGroup(UUID groupUuid);

    List<StudentGroupMemberDTO> getMembers(UUID groupUuid);

    List<StudentGroupMemberDTO> addMembers(UUID groupUuid, List<UUID> studentUuids);

    void removeMember(UUID groupUuid, UUID studentUuid);
}
