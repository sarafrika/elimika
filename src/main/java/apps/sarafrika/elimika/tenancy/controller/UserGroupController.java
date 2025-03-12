package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserGroupDTO;
import apps.sarafrika.elimika.tenancy.services.UserGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/user-groups")
@RequiredArgsConstructor
@Tag(name = "User Group API", description = "Endpoints for managing user groups")
public class UserGroupController {

    private final UserGroupService userGroupService;

    @Operation(summary = "Create a new user group")
    @PostMapping @PreAuthorize("hasAuthority('user_group:create')")
    public ResponseEntity<ApiResponse<UserGroupDTO>> createUserGroup(@Valid @RequestBody UserGroupDTO userGroupDTO) {
        UserGroupDTO createdGroup = userGroupService.createUserGroup(userGroupDTO);
        return ResponseEntity.status(201).body(ApiResponse.success(createdGroup, "User group created successfully"));
    }

    @Operation(summary = "Get a user group by UUID")
    @GetMapping("/{uuid}") @PreAuthorize("hasAuthority('user_group:read')")
    public ResponseEntity<ApiResponse<UserGroupDTO>> getUserGroupByUuid(@PathVariable UUID uuid) {
        UserGroupDTO userGroup = userGroupService.getUserGroupByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(userGroup, "User group retrieved successfully"));
    }

    @Operation(summary = "Get all user groups for an organisation")
    @GetMapping("/organisation/{organisationUuid}") @PreAuthorize("hasAuthority('user_group:read_all')")
    public ResponseEntity<ApiResponse<PagedDTO<UserGroupDTO>>> getUserGroupsByOrganisation(
            @PathVariable UUID organisationUuid, Pageable pageable) {
        Page<UserGroupDTO> userGroups = userGroupService.getUserGroupsByOrganisation(organisationUuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(userGroups, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "User groups retrieved successfully"));
    }

    @Operation(summary = "Update a user group by UUID")
    @PutMapping("/{uuid}") @PreAuthorize("hasAuthority('user_group:update')")
    public ResponseEntity<ApiResponse<UserGroupDTO>> updateUserGroup(
            @PathVariable UUID uuid, @Valid @RequestBody UserGroupDTO userGroupDTO) {
        UserGroupDTO updatedGroup = userGroupService.updateUserGroup(uuid, userGroupDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedGroup, "User group updated successfully"));
    }

    @Operation(summary = "Delete a user group by UUID")
    @DeleteMapping("/{uuid}") @PreAuthorize("hasAuthority('user_group:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteUserGroup(@PathVariable UUID uuid) {
        userGroupService.deleteUserGroup(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "User group deleted successfully"));
    }

    @Operation(summary = "Add users to a user group")
    @PostMapping("/{uuid}/users") @PreAuthorize("hasAuthority('user_group:update')")
    public ResponseEntity<ApiResponse<Void>> addUsersToGroup(@PathVariable UUID uuid, @RequestBody List<UUID> userUuids) {
        userGroupService.addUsersToGroup(uuid, userUuids);
        return ResponseEntity.ok(ApiResponse.success(null, "Users added to user group successfully"));
    }

    @Operation(summary = "Remove users from a user group")
    @DeleteMapping("/{uuid}/users") @PreAuthorize("hasAuthority('user_group:update')")
    public ResponseEntity<ApiResponse<Void>> removeUsersFromGroup(@PathVariable UUID uuid, @RequestBody List<UUID> userUuids) {
        userGroupService.removeUsersFromGroup(uuid, userUuids);
        return ResponseEntity.ok(ApiResponse.success(null, "Users removed from user group successfully"));
    }

    @Operation(summary = "Assign roles to a user group")
    @PostMapping("/{uuid}/roles") @PreAuthorize("hasAuthority('user_group:update')")
    public ResponseEntity<ApiResponse<Void>> assignRolesToGroup(@PathVariable UUID uuid, @RequestBody List<UUID> roleUuids) {
        userGroupService.assignRolesToGroup(uuid, roleUuids);
        return ResponseEntity.ok(ApiResponse.success(null, "Roles assigned to user group successfully"));
    }

    @Operation(summary = "Remove roles from a user group")
    @DeleteMapping("/{uuid}/roles") @PreAuthorize("hasAuthority('user_group:update')")
    public ResponseEntity<ApiResponse<Void>> removeRolesFromGroup(@PathVariable UUID uuid, @RequestBody List<UUID> roleUuids) {
        userGroupService.removeRolesFromGroup(uuid, roleUuids);
        return ResponseEntity.ok(ApiResponse.success(null, "Roles removed from user group successfully"));
    }

    @Operation(summary = "Search user groups",
            description = "Fetches a paginated list of user groups based on optional filters. " +
                    "Supports pagination and sorting.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of user groups matching the search criteria")
    @GetMapping("search") @PreAuthorize("hasAuthority('user_group:read_all')")
    public ResponseEntity<ApiResponse<PagedDTO<UserGroupDTO>>> search(
            @RequestParam(required = false) Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(userGroupService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "User groups search successful"));
    }

    @Operation(summary = "Get users for a user group")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping("{uuid}/users") @PreAuthorize("hasAuthority('user_group:read_all')")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getUsersForUserGroup(@PathVariable UUID uuid, Pageable pageable) {
        Page<UserDTO> users = userGroupService.getUsersForUserGroup(uuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(users, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Users retrieved successfully"));
    }

    @Operation(summary = "Get roles for a user group")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    @GetMapping("{uuid}/roles") @PreAuthorize("hasAuthority('user_group:read_all')")
    public ResponseEntity<ApiResponse<PagedDTO<RoleDTO>>> getRolesForUserGroup(@PathVariable UUID uuid, Pageable pageable) {
        Page<RoleDTO> roles = userGroupService.getRolesForUserGroup(uuid, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(roles, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Roles retrieved successfully"));
    }
}
