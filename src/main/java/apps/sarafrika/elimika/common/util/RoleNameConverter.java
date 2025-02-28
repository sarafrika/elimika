package apps.sarafrika.elimika.common.util;

import apps.sarafrika.elimika.tenancy.entity.Permission;

public class RoleNameConverter {
    public static String createRoleName(Permission permission) {
        return permission.getModuleName().toLowerCase() + "-" + permission.getPermissionName().toLowerCase();
    }
}
