package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.entity.Role;

public class RoleFactory {

    public static RoleDTO toDTO(Role role) {

        return new RoleDTO(
                role.getUuid(),
                role.getOrganisationUuid(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                null,
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
