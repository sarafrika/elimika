package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.UserGroupDTO;
import apps.sarafrika.elimika.tenancy.entity.UserGroup;

public class UserGroupFactory {
    public static UserGroupDTO toDTO(UserGroup userGroup) {
        return new UserGroupDTO(
                userGroup.getUuid(),
                userGroup.getOrganisation().getUuid(),
                userGroup.getName(),
                userGroup.isActive(),
                userGroup.getCreatedDate(),
                userGroup.getLastModifiedDate()
        );
    }

    public static UserGroup toEntity(UserGroupDTO userGroupDTO) {
        UserGroup userGroup = new UserGroup();
        userGroup.setName(userGroupDTO.name());
        userGroup.setActive(userGroupDTO.active());
        return userGroup;
    }
}
