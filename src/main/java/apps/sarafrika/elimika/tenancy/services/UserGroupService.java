package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserGroupDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserGroupService {
    UserGroupDTO createUserGroup(UserGroupDTO userGroupDTO);

    UserGroupDTO getUserGroupByUuid(UUID uuid);

    Page<UserGroupDTO> getUserGroupsByOrganisation(UUID organisationUuid, Pageable pageable);

    UserGroupDTO updateUserGroup(UUID uuid, UserGroupDTO userGroupDTO);

    void deleteUserGroup(UUID uuid);

    void addUsersToGroup(UUID groupUuid, List<UUID> userUuids);

    void removeUsersFromGroup(UUID groupUuid, List<UUID> userUuids);

    void assignRolesToGroup(UUID groupUuid, List<UUID> roleUuids);

    void removeRolesFromGroup(UUID groupUuid, List<UUID> roleUuids);

    Page<UserGroupDTO> search(Map<String, String> searchParams, Pageable pageable);

    Page<UserDTO> getUsersForUserGroup(UUID uuid, Pageable pageable);

    Page<RoleDTO> getRolesForUserGroup(UUID uuid, Pageable pageable);
}
