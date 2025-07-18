package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.common.dto.ApiResponse;
import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageFileNotFoundException;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.services.InvitationService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Users API", description = "Complete user management including profile management, domain assignments, and user-specific invitations")
class UserController {
    private final UserService userService;
    private final InvitationService invitationService;
    private final StorageService storageService;
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

        try {
            String profileImageFolder = storageProperties.getFolders().getProfileImages();
            String fullPath = profileImageFolder + "/" + fileName;

            Resource resource = storageService.load(fullPath);

            String contentType = storageService.getContentType(fullPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (StorageFileNotFoundException e) {
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Operation(summary = "Delete a user by UUID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "UUID of the user to delete. This will remove the user and all their organization relationships.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
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
            @Parameter(description = "Optional search parameters for filtering users. " +
                    "Supported filters: firstName, lastName, email, phoneNumber, active, gender. " +
                    "Example: firstName=John&active=true",
                    example = "firstName=John&active=true")
            @RequestParam(required = false) Map<String, String> searchParams,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(userService.search(searchParams, pageable), ServletUriComponentsBuilder
                        .fromCurrentRequestUri()
                        .build()
                        .toUriString()),
                "Users search successful"));
    }

    // ================================
    // USER INVITATIONS MANAGEMENT
    // ================================

    @Operation(
            summary = "Get pending invitations for user by email",
            description = "Retrieves all pending invitations sent to a specific user's email address across all organizations and branches. " +
                    "This endpoint helps users see all outstanding invitations they have received. " +
                    "Only returns invitations with PENDING status that haven't expired."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending invitations retrieved successfully (may be empty list)")
    @GetMapping("/{uuid}/invitations/pending")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> getPendingInvitationsForUser(
            @Parameter(description = "UUID of the user to get pending invitations for. The system will use the user's email to find invitations.",
                    example = "550e8400-e29b-41d4-a716-446655440001", required = true)
            @PathVariable UUID uuid) {
        UserDTO user = userService.getUserByUuid(uuid);
        List<InvitationDTO> invitations = invitationService.getPendingInvitationsForEmail(user.email());
        return ResponseEntity.ok(ApiResponse.success(invitations, "Pending invitations retrieved successfully"));
    }

    @Operation(
            summary = "Get invitations sent by user",
            description = "Retrieves all invitations that have been sent by a specific user across all organizations and branches. " +
                    "This endpoint helps users track invitations they have created. " +
                    "Results are ordered by creation date (most recent first) and include all invitation statuses."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User's sent invitations retrieved successfully (may be empty list)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{uuid}/invitations/sent")
    public ResponseEntity<ApiResponse<List<InvitationDTO>>> getInvitationsSentByUser(
            @Parameter(description = "UUID of the user to retrieve sent invitations for. Must be an existing user.",
                    example = "550e8400-e29b-41d4-a716-446655440004", required = true)
            @PathVariable UUID uuid) {
        List<InvitationDTO> invitations = invitationService.getInvitationsSentByUser(uuid);
        return ResponseEntity.ok(ApiResponse.success(invitations, "Sent invitations retrieved successfully"));
    }

    @Operation(
            summary = "Accept invitation by token",
            description = "Accepts a pending invitation for the specified user using the unique token from the invitation email. " +
                    "This creates the user-organization relationship with the specified role and sends confirmation emails. " +
                    "The invitation must be valid (not expired, not already accepted/declined) and the user email must match the invitation recipient."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation accepted successfully, user added to organization/branch with specified role")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid token, expired invitation, user email mismatch, or user already member of organization")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation token not found or user not found")
    @PostMapping("/{uuid}/invitations/accept")
    public ResponseEntity<ApiResponse<UserDTO>> acceptInvitation(
            @Parameter(description = "UUID of the user who is accepting the invitation. The user's email must match the invitation recipient email for security.",
                    example = "550e8400-e29b-41d4-a716-446655440005", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Unique invitation token from the invitation email URL. This is the 64-character token that identifies the specific invitation.",
                    example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz0123456789abcd", required = true)
            @RequestParam("token") String token) {
        UserDTO user = invitationService.acceptInvitation(token, uuid);
        return ResponseEntity.ok(ApiResponse.success(user, "Invitation accepted successfully"));
    }

    @Operation(
            summary = "Decline invitation by token",
            description = "Declines a pending invitation for the specified user using the unique token from the invitation email. " +
                    "This marks the invitation as declined and sends notification emails to the inviter. " +
                    "The invitation must be valid (not expired, not already accepted/declined) and the user email must match the invitation recipient."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invitation declined successfully, notifications sent to inviter")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid token, expired invitation, or user email mismatch")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invitation token not found or user not found")
    @PostMapping("/{uuid}/invitations/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(
            @Parameter(description = "UUID of the user who is declining the invitation. The user's email must match the invitation recipient email for security.",
                    example = "550e8400-e29b-41d4-a716-446655440005", required = true)
            @PathVariable UUID uuid,
            @Parameter(description = "Unique invitation token from the invitation email URL. This is the 64-character token that identifies the specific invitation.",
                    example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz0123456789abcd", required = true)
            @RequestParam("token") String token) {
        invitationService.declineInvitation(token, uuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Invitation declined successfully"));
    }
}