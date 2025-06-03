package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.PermissionDTO;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.services.PermissionService;
import apps.sarafrika.elimika.tenancy.services.RoleEvaluationService;
import apps.sarafrika.elimika.tenancy.services.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @CrossOrigin
@RequestMapping("api/v1/roles")
@RequiredArgsConstructor @Tag(name = "Roles API", description = "Roles related operations")
public class RoleController {
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RoleEvaluationService roleEvaluationService;

    @Operation(summary = "Create a new role for an organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Role created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        RoleDTO created = roleService.createRole(roleDTO);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Role created successfully"));
    }

    @Operation(summary = "Get a role by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleByUuid(@PathVariable UUID uuid) {
        RoleDTO role = roleService.getRoleByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(role, "Role retrieved successfully"));
    }

    @Operation(summary = "Get all roles for a specific organisation")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    @GetMapping("/organisation/{organisationUid}")
    public ResponseEntity<ApiResponse<PagedDTO<RoleDTO>>> getRolesByOrganisation(@PathVariable UUID organisationUid, Pageable pageable) {
        Page<RoleDTO> roles = roleService.getRolesByOrganisation(organisationUid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(roles, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Roles retrieved successfully"));
    }

    @Operation(summary = "Update a role by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<RoleDTO>> updateRole(
            @PathVariable UUID uuid, @Valid @RequestBody RoleDTO roleDTO) {
        RoleDTO updated = roleService.updateRole(uuid, roleDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Role updated successfully"));
    }

    @Operation(summary = "Delete a role by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID uuid) {
        roleService.deleteRole(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Role deleted successfully"));
    }

    @Operation(summary = "Search roles",
            description = "Fetches a paginated list of roles based on optional filters. " +
                    "Supports pagination and sorting.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of roles matching the search criteria")
    @GetMapping("search")
    public ResponseEntity<ApiResponse<PagedDTO<RoleDTO>>> searchRoles(
            @RequestParam(required = false) Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(roleService.search(searchParams, pageable),
                        ServletUriComponentsBuilder
                                .fromCurrentRequestUri()
                                .build()
                                .toUriString()),
                "Roles search successful"));
    }

    @Operation(summary = "Fetch all available permissions",
            description = "Retrieve a list of all available permissions that can be assigned to roles.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permissions retrieved successfully")
    @GetMapping("permissions")
    public ResponseEntity<ApiResponse<List<PermissionDTO>>> getAllPermissions() {
        List<PermissionDTO> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions, "Permissions retrieved successfully"));
    }

    @Operation(summary = "Get effective roles for a user",
            description = "Fetches roles for a user, considering both direct and group assignments, with precedence given to user roles.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    @GetMapping("/users/{userUuid}/roles")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getEffectiveRolesForUser(@PathVariable UUID userUuid) {
        List<RoleDTO> roles = roleEvaluationService.getEffectiveRolesForUser(userUuid);
        return ResponseEntity.ok(ApiResponse.success(roles, "Effective roles retrieved successfully"));
    }


}
