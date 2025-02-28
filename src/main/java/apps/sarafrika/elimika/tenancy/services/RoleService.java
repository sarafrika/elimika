package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.PermissionDTO;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RoleService {
    RoleDTO createRole(RoleDTO roleDTO);

    RoleDTO getRoleByUuid(UUID uuid);

    Page<RoleDTO> getRolesByOrganisation(UUID organisationUid, Pageable pageable);

    RoleDTO updateRole(UUID uuid, RoleDTO roleDTO);

    void deleteRole(UUID uuid);

    Page<RoleDTO> search(Map<String, String> searchParams, Pageable pageable);

    RoleDTO addPermissionsToRole(UUID roleUuid, List<UUID> permissionUuids);

    RoleDTO removePermissionsFromRole(UUID roleUuid, List<UUID> permissionUuids);

    List<PermissionDTO> getPermissionsByRole(UUID roleUuid);
}
