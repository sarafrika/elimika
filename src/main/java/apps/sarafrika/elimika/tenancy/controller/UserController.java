package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users API", description = "Users related operations")
class UserController {
    private final UserService userService;
    private final StorageService storageService;

    @Operation(summary = "Get a user by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserByUuid(@PathVariable UUID uuid) {
        UserDTO user = userService.getUserByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @Operation(summary = "Get users by organisation ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getUsersByOrganisation(@PathVariable UUID organisationId, Pageable pageable) {
        Page<UserDTO> users = userService.getUsersByOrganisation(organisationId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(users, ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Users retrieved successfully"));
    }

    @Operation(summary = "Update a user by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping(value = "/{uuid}", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable UUID uuid, @Valid @RequestParam(name = "user")  UserDTO userDTO, @RequestParam(value = "profile_image", required = false)
            MultipartFile profileImage) {
        UserDTO updated = userService.updateUser(uuid, userDTO, profileImage);
        return ResponseEntity.ok(ApiResponse.success(updated, "User updated successfully"));
    }

    @Operation(summary = "Delete a user by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID uuid) {
        userService.deleteUser(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @Operation(summary = "Search users",
            description = "Fetches a paginated list of users based on optional filters. " +
                    "Supports pagination and sorting.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of users matching the search criteria")
    @GetMapping("search")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> search(
            @RequestParam(required = false) Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(userService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Users search successful"));
    }

    @Operation(summary = "Get user profile image by file name")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile image retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile image not found")
    @GetMapping("profile-image/{fileName}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String fileName) {
        return ResponseEntity.ok().body(storageService.load(fileName));
    }
}