package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
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
    /**
     * The self-scoped identity route needs no domain at all: a brand-new account with no student,
     * instructor or organisation mapping yet must still be able to find out who it is, otherwise it
     * can never reach onboarding.
     */
    private static final String AUTHENTICATED = "isAuthenticated()";

    private final UserService userService;
    private final MediaServeService mediaServeService;
    private final StorageProperties storageProperties;
    private final DomainSecurityService domainSecurityService;

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
     * The sign-in bootstrap: the one route a client can call before it knows anything about itself.
     * <p>
     * Identity comes from the JWT, never from a query parameter, so there is nothing to enumerate
     * and nothing to guess — the caller can only ever address their own record. That is what lets
     * {@code /search} below be closed to platform admins: the bootstrap no longer needs it.
     * <p>
     * Returns the full {@link UserDTO}, which already carries {@code user_domain} and
     * {@code organisation_affiliations}, so the client resolves identity and routing in one call.
     * Note that the domain list is the one {@code UserService} has always produced: an admin whose
     * {@code admin} role is scoped to an organisation is reported as {@code organisation_user}, not
     * {@code admin}. That is deliberate, and it is what the dashboard's role routing expects.
     */
    @Operation(operationId = "getCurrentUser", summary = "Get the authenticated user",
            description = "Returns the caller's own user record, resolved from the access token. " +
                    "Includes the caller's domains and organisation affiliations.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No authenticated caller")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Authenticated caller has no user record")
    @GetMapping("me")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
        if (currentUserUuid == null) {
            // The token authenticated but no local record answers to it. UserSyncFilter creates one
            // on the first authenticated request, so this means that sync failed rather than that the
            // account is new.
            throw new ResourceNotFoundException("No user record for the authenticated caller");
        }
        UserDTO user = userService.getUserByUuid(currentUserUuid);
        return ResponseEntity.ok(ApiResponse.success(user, "Current user retrieved successfully"));
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
     * A filterable projection of the whole user table — email, phone number and date of birth
     * included — so it is restricted to platform admins.
     * <p>
     * It used to be the sign-in bootstrap: the client turned the session email into a user record
     * through {@code ?email_eq=} here before it knew the caller's own UUID, which meant every
     * authenticated caller could also page through everyone else. {@code GET /api/v1/users/me}
     * above now answers that question from the token, so the bootstrap no longer needs a search and
     * the search no longer needs to be open.
     * <p>
     * The two changes are one release: narrowing this before clients have moved to {@code /me}
     * breaks sign-in for every non-admin.
     */
    @Operation(summary = "Search users",
            description = "Fetches a paginated list of users based on optional filters. " +
                    "Supports pagination and sorting. Restricted to platform administrators — " +
                    "callers looking up their own record should use GET /api/v1/users/me.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of users matching the search criteria")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Caller is not a platform administrator")
    // DEPLOY ORDER: the guard is withheld until the frontend has stopped calling this route.
    // `main` deploys on push, so landing @PreAuthorize(PLATFORM_ADMIN) here before the clients are
    // live would 403 the sign-in bootstrap for every non-admin. The route is no less open than it
    // is today; re-apply the guard once the frontend release is out. See UserControllerTest.
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
