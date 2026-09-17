package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.timetabling.dto.InstructorStudentPageDTO;
import apps.sarafrika.elimika.timetabling.service.InstructorStudentRosterService;
import apps.sarafrika.elimika.timetabling.service.InstructorStudentRosterService.InstructorStudentRoster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping(OrganisationInstructorStudentController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Organisation Instructor Students", description = "Students an instructor teaches in an organisation's classes")
public class OrganisationInstructorStudentController {

    public static final String API_ROOT_PATH = "/api/v1/organisations/{organisationUuid}/instructors/{instructorUuid}/students";

    private final InstructorStudentRosterService rosterService;

    @Operation(
            summary = "List the students an instructor teaches in the organisation's classes",
            description = "One row per student per class, for classes the organisation owns and the instructor is "
                    + "instructor of record for. Only managers of the organisation (and platform admins) may ask; "
                    + "class_options lists every such class with students and student_count the distinct students across them, "
                    + "whatever the filters.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Students retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping
    @PreAuthorize("@domainSecurityService.isPlatformAdmin() or @domainSecurityService.managesOrganisation(#organisationUuid)")
    public ResponseEntity<ApiResponse<InstructorStudentPageDTO>> listInstructorStudents(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID instructorUuid,
            @Parameter(description = "Case-insensitive part of the student's name")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Only students of this class")
            @RequestParam(value = "class_definition_uuid", required = false) UUID classDefinitionUuid,
            @Parameter(description = "Zero-based page number")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size, at most 100")
            @RequestParam(value = "size", defaultValue = "20") int size) {
        InstructorStudentRoster roster = rosterService.listInstructorStudents(
                organisationUuid, instructorUuid, search, classDefinitionUuid, page, size);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(
                InstructorStudentPageDTO.from(roster.students(), roster.classOptions(), roster.studentCount(), baseUrl),
                "Instructor students retrieved successfully"));
    }
}
