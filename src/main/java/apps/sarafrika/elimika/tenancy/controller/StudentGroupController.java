package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.AddGroupMembersRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateStudentGroupRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupDTO;
import apps.sarafrika.elimika.tenancy.dto.StudentGroupMemberDTO;
import apps.sarafrika.elimika.tenancy.services.StudentGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Tag(name = "Student Groups API", description = "Organisation-scoped student groups (cohorts/streams) and membership")
public class StudentGroupController {

    /**
     * Student groups are organisation property: who is in a cohort is roster data, and changing a
     * cohort changes who gets enrolled downstream. Both are scoped to the owning organisation, which
     * for the group-scoped routes has to be resolved from the group itself since the path omits it.
     */
    private static final String READ_ORGANISATION =
            "@organisationSecurityService.canReadOrganisation(#organisationUuid)";
    private static final String MANAGE_ORGANISATION =
            "@organisationSecurityService.canManageOrganisation(#organisationUuid)";
    private static final String READ_GROUP =
            "@organisationSecurityService.canReadGroup(#groupUuid)";
    private static final String MANAGE_GROUP =
            "@organisationSecurityService.canManageGroup(#groupUuid)";

    private final StudentGroupService studentGroupService;

    @Operation(summary = "List student groups for an organisation", description = "Returns all student groups for the organisation with member counts.")
    @GetMapping("/organisations/{organisationUuid}/student-groups")
    @PreAuthorize(READ_ORGANISATION)
    public ResponseEntity<ApiResponse<List<StudentGroupDTO>>> listGroups(@PathVariable UUID organisationUuid) {
        List<StudentGroupDTO> groups = studentGroupService.getGroupsForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(groups, "Student groups retrieved successfully"));
    }

    @Operation(summary = "Create a student group", description = "Creates a new student group within the organisation.")
    @PostMapping("/organisations/{organisationUuid}/student-groups")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<StudentGroupDTO>> createGroup(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody CreateStudentGroupRequestDTO request) {
        StudentGroupDTO created = studentGroupService.createGroup(organisationUuid, request);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Student group created successfully"));
    }

    @Operation(summary = "Delete a student group", description = "Deletes a student group and its membership rows.")
    @DeleteMapping("/student-groups/{groupUuid}")
    @PreAuthorize(MANAGE_GROUP)
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable UUID groupUuid) {
        studentGroupService.deleteGroup(groupUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Student group deleted successfully"));
    }

    @Operation(summary = "List group members", description = "Returns the student membership rows for a group.")
    @GetMapping("/student-groups/{groupUuid}/members")
    @PreAuthorize(READ_GROUP)
    public ResponseEntity<ApiResponse<List<StudentGroupMemberDTO>>> listMembers(@PathVariable UUID groupUuid) {
        List<StudentGroupMemberDTO> members = studentGroupService.getMembers(groupUuid);
        return ResponseEntity.ok(ApiResponse.success(members, "Group members retrieved successfully"));
    }

    @Operation(summary = "Add students to a group", description = "Adds one or more students to the group (idempotent per student).")
    @PostMapping("/student-groups/{groupUuid}/members")
    @PreAuthorize(MANAGE_GROUP)
    public ResponseEntity<ApiResponse<List<StudentGroupMemberDTO>>> addMembers(
            @PathVariable UUID groupUuid,
            @Valid @RequestBody AddGroupMembersRequestDTO request) {
        List<StudentGroupMemberDTO> members = studentGroupService.addMembers(groupUuid, request.studentUuids());
        return ResponseEntity.ok(ApiResponse.success(members, "Members added successfully"));
    }

    @Operation(summary = "Remove a student from a group")
    @DeleteMapping("/student-groups/{groupUuid}/members/{studentUuid}")
    @PreAuthorize(MANAGE_GROUP)
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID groupUuid,
            @PathVariable UUID studentUuid) {
        studentGroupService.removeMember(groupUuid, studentUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed successfully"));
    }
}
