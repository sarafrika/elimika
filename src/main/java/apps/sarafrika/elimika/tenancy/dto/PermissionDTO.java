package apps.sarafrika.elimika.tenancy.dto;

import java.util.UUID;

public record PermissionDTO(UUID uuid, String moduleName, String permissionName, String description) {
}
