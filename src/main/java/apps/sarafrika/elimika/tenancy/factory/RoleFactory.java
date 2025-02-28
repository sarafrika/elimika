package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.PermissionDTO;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.entity.Role;

import java.util.List;
import java.util.stream.Collectors;

public class RoleFactory {

    public static RoleDTO toDTO(Role role) {
        List<PermissionDTO> permissions = role.getPermissions().stream()
                .map(permission -> new PermissionDTO(
                        permission.getUuid(),
                        permission.getModuleName(),
                        permission.getPermissionName(),
                        permission.getDescription()
                ))
                .collect(Collectors.toList());

        return new RoleDTO(
                role.getUuid(),
                role.getOrganisationUuid(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                permissions,
                role.getCreatedDate(),
                role.getLastModifiedDate()
        );
    }

    public static Role toEntity(RoleDTO dto) {
        Role role = new Role();
        role.setUuid(dto.uuid());
        role.setOrganisationUuid(dto.organisationUuid());
        role.setName(dto.name());
        role.setActive(dto.active());
        role.setDescription(dto.description());

        return role;
    }
}
