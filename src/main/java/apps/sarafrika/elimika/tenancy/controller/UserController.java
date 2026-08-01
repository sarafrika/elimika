package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users API", description = "Complete user management including profile management and domain assignments")
class UserController {

    /**
     * Nobody but the account holder edits an account. Support still needs a way in, so a platform
     * admin is allowed through; an org-scoped admin is not, because holding {@code admin} inside one
     * organisation says nothing about a user's personal record.
     * <p>
     * Two constants rather than one because the path variable is spelled differently on the
     * profile-image route, and a {@code #name} that does not match the Java parameter fails at
     * request time rather than at compile time.
     */
    private static final String SELF_OR_PLATFORM_ADMIN =
            "@domainSecurityService.isPlatformAdmin() or #uuid == @domainSecurityService.getCurrentUserUuid()";
    private static final String SELF_OR_PLATFORM_ADMIN_BY_USER_UUID =
            "@domainSecurityService.isPlatformAdmin() or #userUuid == @domainSecurityService.getCurrentUserUuid()";
    /**
     * An unfiltered listing of every account on the platform. No caller in the product needs it, so
     * it is restricted rather than narrowed.
     */
    private static final String PLATFORM_ADMIN = "@domainSecurityService.isPlatformAdmin()";

    private final UserService userService;
    private final MediaServeService mediaServeService;
    private final StorageProperties storageProperties;

    // ================================
    // CORE USER MANAGEMENT
    // ================================

    @Operation(summary = "Get all users",
            description = "Fetches a paginated list of all users in the system. " +
                    "Supports pagination and sorting by any user field.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of all users retrieved successfully")
    @GetMapping
    @PreAuthorize(PLATFORM_ADMIN)
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> getAllUsers(
            @Parameter(description = "Pagination and sorting parameters. " +
                    "Default page size is 20. Supports sorting by fields like firstName, lastName, email, createdAt. " +
                    "Example: ?page=0&size=10&sort=firstName,asc")
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(userService.getAllUsers(pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Users retrieved successfully"));
    }

    /**
     * Deliberately left at the global {@code authenticated()} baseline.
     * <p>
     * This is the platform's people directory, not a private record: instructor cards, student
     * lists, class waiting lists, enrolment tables, the calendar and the public profile page all
     * resolve a name and an avatar through it, for users the caller has no organisational
     * relationship with. Narrowing it to self-or-admin would blank out most of the product for
     * ordinary learners and instructors.
     * <p>
     * The real exposure here is the shape of the payload — {@link UserDTO} carries email, phone
     * number and date of birth alongside the display fields — and the fix for that is redacting the
     * response for non-self callers, not refusing the request. Tracked separately.
     */
    @Operation(summary = "Get a user by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserByUuid(
            @Parameter(description = "UUID of the user to retrieve. Must be an existing user identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
        UserDTO user = userService.getUserByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @Operation(summary = "Update a user by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PutMapping(value = "/{uuid}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @PreAuthorize(SELF_OR_PLATFORM_ADMIN)
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @Parameter(description = "UUID of the user to update. Must be an existing user identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid,
            @Valid @RequestBody UserDTO userDTO) {
        UserDTO updated = userService.updateUser(uuid, userDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "User updated successfully"));
    }

    @Operation(summary = "Upload User's Profile Image")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile Image Uploaded successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data")
    @PostMapping(value = "{userUuid}/profile-image", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    @PreAuthorize(SELF_OR_PLATFORM_ADMIN_BY_USER_UUID)
    public ResponseEntity<UserDTO> uploadProfileImage(
            @Parameter(description = "UUID of the user", required = true)
            @PathVariable UUID userUuid,
            @Parameter(description = "Profile image file to upload", required = true)
            @RequestParam("profileImage") MultipartFile profileImage) {

        try {
            UserDTO updatedUser = userService.uploadProfileImage(userUuid, profileImage);

            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Public by design: {@code SecurityConfiguration} explicitly permits
     * {@code GET /api/v1/users/profile-image/**} so avatars render in unauthenticated contexts. A
     * method-level guard here would override that and break them, so there is none.
     */
    @Operation(summary = "Get user profile image by file name")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile image retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile image not found")
    @GetMapping("profile-image/{fileName}")
    public ResponseEntity<Resource> getProfileImage(
            @Parameter(
                    description = "Name of the profile image file to retrieve. Format: profile_images_uuid.extension",
                    example = "profile_images_c5be646f-34c3-4782-9be4-dfbe93fe06b6.png",
                    required = true
            )
            @PathVariable String fileName) {
        // Legacy endpoint: historical URLs carry only the bare filename, current keys
        // include the folder. Serve either form.
        String profileImageFolder = storageProperties.getFolders().getProfileImages();
        return mediaServeService.serve(profileImageFolder + "/" + fileName, fileName);
    }


    @Operation(summary = "Delete a user by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @DeleteMapping("/{uuid}")
    @PreAuthorize(SELF_OR_PLATFORM_ADMIN)
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "UUID of the user to delete. This will remove the user and all their organisation relationships.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
        userService.deleteUser(uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    /**
     * Deliberately left at the global {@code authenticated()} baseline, and the one endpoint on this
     * controller that still wants narrowing.
     * <p>
     * It is the sign-in bootstrap: the client turns the session email into a user record through
     * {@code ?email_eq=} here before it knows the caller's own UUID, so every user hits it on every
     * page load. Restricting it to platform admins — the usual answer for an unfiltered
     * enumeration — would lock every non-admin out of the product entirely.
     * <p>
     * That leaves a real hole: any authenticated caller can page through the whole user table,
     * emails, phone numbers and dates of birth included. Closing it properly means a self-scoped
     * "who am I" route for the bootstrap and admin-only access to the general search, which is a
     * coordinated backend/frontend change rather than an annotation.
     */
    @Operation(summary = "Search users",
            description = "Fetches a paginated list of users based on optional filters. " +
                    "Supports pagination and sorting.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of users matching the search criteria")
    @GetMapping("search")
    public ResponseEntity<ApiResponse<PagedDTO<UserDTO>>> search(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam() Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(userService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Users search successful"));
    }

}
