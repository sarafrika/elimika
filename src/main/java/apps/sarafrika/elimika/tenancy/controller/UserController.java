package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.UserContactSecurityService;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserSummaryDTO;
import apps.sarafrika.elimika.tenancy.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
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
    /**
     * How many users one directory lookup may resolve. Chosen so the worst-case query string
     * (36 characters plus a separator per UUID, ~3.7 KB) stays comfortably inside the 8 KB request
     * line most proxies allow, and so a caller cannot turn one request into an unbounded scan.
     */
    static final int MAX_DIRECTORY_UUIDS = 100;

    private final UserService userService;
    private final MediaServeService mediaServeService;
    private final StorageProperties storageProperties;
    private final DomainSecurityService domainSecurityService;
    /**
     * Not a {@code @PreAuthorize} bean reference: the answer selects a payload shape rather than
     * allowing or denying the request, so it is asked in the handler. It lives in a service all the
     * same, because an authorization predicate is domain logic and the next route that needs the
     * same question must get the same answer.
     */
    private final UserContactSecurityService userContactSecurityService;

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
     * Stays open to every authenticated caller, but no longer answers everyone the same way.
     * <p>
     * This is the platform's people directory, not a private record: instructor cards, student
     * lists, class waiting lists, enrolment tables, the calendar and the public profile page all
     * resolve a name and an avatar through it, for users the caller has no organisational
     * relationship with. Narrowing it to self-or-admin would blank out most of the product for
     * ordinary learners and instructors, so the request is never refused on identity grounds.
     * <p>
     * What changes with the caller is the shape of the payload. {@link UserDTO} carries email,
     * phone number, date of birth, username and the organisation affiliations, and none of that
     * belongs in a roster row. Who has a claim to it is
     * {@link UserContactSecurityService#canReadContactDetails(UUID) one question asked in one place}
     * — the account holder, a platform administrator, a manager of an organisation the account
     * belongs to, or somebody who stands in a working relationship with them, such as the
     * instructor whose register they are on or the course creator whose course they applied to
     * teach. Everyone else receives the same {@link UserSummaryDTO} projection that
     * {@link #getUserDirectory} serves: the display identity, and nothing that identifies the
     * person outside the platform.
     * <p>
     * Neither branch is free — the contact test costs at least one lookup, and the full record
     * costs the domain and affiliation fan-out on top — but the whole answer is memoised for the
     * request, so a handler that consults it more than once pays once.
     * <p>
     * Both branches answer 404 for an unknown UUID. That is not a leak: any authenticated caller
     * may learn that an account exists, because that is what a directory is for.
     */
    @Operation(summary = "Get a user by UUID",
            description = "Returns the full User record to the account holder, to platform administrators, "
                    + "to a manager of one of the account's organisations, and to a caller with a working "
                    + "relationship to them - the instructor whose class they are enrolled or waitlisted on, "
                    + "or the course creator whose course or programme they are enrolled on or have applied "
                    + "to teach. Every other authenticated caller receives the UserSummary directory "
                    + "projection: display identity only, with no email, phone number, date of birth or "
                    + "username.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "User retrieved successfully. `data` is a User for privileged callers and a "
                    + "UserSummary otherwise.",
            content = @Content(mediaType = APPLICATION_JSON_VALUE, schemaProperties = {
                    @SchemaProperty(name = "success", schema = @Schema(type = "boolean")),
                    @SchemaProperty(name = "data", schema = @Schema(oneOf = {UserDTO.class, UserSummaryDTO.class})),
                    @SchemaProperty(name = "message", schema = @Schema(type = "string"))
            }))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No authenticated caller")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{uuid}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<ApiResponse<?>> getUserByUuid(
            @Parameter(description = "UUID of the user to retrieve. Must be an existing user identifier.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
        if (userContactSecurityService.canReadContactDetails(uuid)) {
            UserDTO user = userService.getUserByUuid(uuid);
            return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
        }
        UserSummaryDTO summary = userService.getUserDirectory(List.of(uuid)).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User not found for UUID: " + uuid));
        return ResponseEntity.ok(ApiResponse.success(summary, "User retrieved successfully"));
    }

    /**
     * The bulk form of {@link #getUserByUuid}, and the reason {@code /search} can finally be closed.
     * <p>
     * Five screens — organisation revenue, the instructor training hub, the instructor student list,
     * the calendar and instructor search — draw dozens of people at once. They were resolving them
     * through {@code /search?uuid_in=}, which is why that route had to stay open to everybody. One
     * request per UUID is not the alternative: that is a per-row fetch across every one of those
     * screens, which this repository's guidelines forbid outright.
     * <p>
     * Two things make this safe to leave at the authenticated baseline where {@code /search} was not.
     * It answers with {@link UserSummaryDTO} rather than {@link UserDTO}, so no email, phone number
     * or date of birth crosses the wire. And it is addressed, not filterable: a caller must already
     * hold the UUIDs it asks about, so there is nothing to enumerate and no way to page the table.
     * That is the same bargain {@code GET /api/v1/users/{uuid}} already makes, on a smaller payload.
     * <p>
     * {@code GET} rather than {@code POST} because the request is a pure read and benefits from
     * being cacheable and idempotent. The cap keeps the URL well inside every proxy's limit:
     * {@value #MAX_DIRECTORY_UUIDS} UUIDs is roughly 3.7 KB of query string, against the 8 KB that
     * nginx and friends allow by default. If the cap ever needs to grow past a few hundred, that is
     * the point to move the list into a POST body — not before.
     * <p>
     * Over-long requests are refused rather than truncated. Silently dropping the tail would hand
     * the client a half-populated map with no signal that anything was missing, and the symptom
     * would surface as blank names on the last rows of a long roster.
     */
    @Operation(operationId = "getUserDirectory", summary = "Look up a batch of users for display",
            description = "Resolves up to " + MAX_DIRECTORY_UUIDS + " user UUIDs to their directory " +
                    "summary — name, avatar and account number — in one request. Returns display " +
                    "identity only; it carries no email, phone number or date of birth. Unknown " +
                    "UUIDs are omitted from the response rather than treated as an error.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Directory summaries for the UUIDs that matched an existing user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "More than " + MAX_DIRECTORY_UUIDS + " UUIDs requested, or a value that is not a UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "No authenticated caller")
    @GetMapping("directory")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<ApiResponse<List<UserSummaryDTO>>> getUserDirectory(
            @Parameter(
                    description = "Comma-separated user UUIDs to resolve. At most "
                            + MAX_DIRECTORY_UUIDS + " per request; chunk larger lists client-side.",
                    example = "550e8400-e29b-41d4-a716-446655440001,550e8400-e29b-41d4-a716-446655440002",
                    required = true
            )
            @RequestParam("uuid_in") List<UUID> uuids) {
        if (uuids.size() > MAX_DIRECTORY_UUIDS) {
            throw new IllegalArgumentException(
                    "A directory lookup accepts at most " + MAX_DIRECTORY_UUIDS + " uuids per request, got "
                            + uuids.size() + ". Split the list into chunks.");
        }
        List<UserSummaryDTO> summaries = userService.getUserDirectory(uuids);
        return ResponseEntity.ok(ApiResponse.success(summaries, "Users retrieved successfully"));
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
     * Its other caller was bulk directory lookup: clients passed {@code ?uuid_in=} a batch of UUIDs
     * to resolve names and avatars, which is why closing this route had to wait.
     * {@link #getUserDirectory} now serves that in a smaller shape, so nothing outside the admin
     * console reaches for a search any more.
     * <p>
     * Ordering matters on the way out: this guard must not ship ahead of the clients that stopped
     * calling the route, or their pages 403.
     */
    @Operation(summary = "Search users",
            description = "Fetches a paginated list of users based on optional filters. " +
                    "Supports pagination and sorting. Restricted to platform administrators — " +
                    "callers looking up their own record should use GET /api/v1/users/me.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Paginated list of users matching the search criteria")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Caller is not a platform administrator")
    @GetMapping("search")
    @PreAuthorize(PLATFORM_ADMIN)
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
