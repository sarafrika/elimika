package apps.sarafrika.elimika.student.controller;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.student.dto.StudentDTO;
import apps.sarafrika.elimika.student.service.StudentDirectoryService;
import apps.sarafrika.elimika.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * The student directory.
 * <p>
 * Reads sit at the authenticated baseline and are <em>projected</em> rather than refused: a caller
 * with no relationship to a learner still gets uuid, user, full name and bio — what a roster row, an
 * enrolment table, a review card or a public profile page draws — and never the guardian names,
 * guardian mobile numbers, demographic tag or audit trail. Which half a caller receives is decided
 * in {@code StudentDirectorySecurityService} and applied in {@code StudentDirectoryService}, so this
 * controller picks nothing.
 * <p>
 * Writes are gated on the relationship itself, and deletion — which strips the {@code student}
 * domain platform-wide — is narrower than the rest.
 */
@RestController
@RequestMapping(StudentController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "Endpoints for managing students")
public class StudentController {
    public static final String API_ROOT_PATH = "/api/v1/students";

    /**
     * Reads are projected, not refused, so the gate is only that somebody is asking. See the class
     * javadoc.
     */
    private static final String AUTHENTICATED = "isAuthenticated()";

    private final StudentService studentService;
    private final StudentDirectoryService studentDirectoryService;

    /**
     * Creates a new student.
     *
     * @param studentDTO The student data to be created.
     * @return The created student DTO.
     */
    @Operation(summary = "Create a new student", description = "Saves a new student record in the system.", responses = {@ApiResponse(responseCode = "201", description = "Student created successfully", content = @Content(schema = @Schema(implementation = StudentDTO.class))), @ApiResponse(responseCode = "400", description = "Invalid request data"), @ApiResponse(responseCode = "403", description = "Caller may not create a student profile for that user")})
    @PostMapping
    @PreAuthorize("@studentDirectorySecurityService.canCreateStudentFor(#studentDTO.userUuid())")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<StudentDTO>> createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO createdStudent = studentService.createStudent(studentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(createdStudent, "Student created successfully"));
    }


    /**
     * Retrieves a student by UUID. Guardian contacts, demographic tag and audit trail are present
     * only for a caller related to the learner; everyone else receives the directory projection.
     *
     * @param uuid The UUID of the student.
     * @return The student DTO if found.
     */
    @Operation(summary = "Get student by ID", description = "Fetches a student by their UUID. Guardian contacts, demographic tag and audit fields are returned only to the learner, an active guardian, a manager of one of the learner's organisations, or a platform admin; other callers receive display identity only.", responses = {@ApiResponse(responseCode = "200", description = "Student found", content = @Content(schema = @Schema(implementation = StudentDTO.class))), @ApiResponse(responseCode = "404", description = "Student not found")})
    @GetMapping("/{uuid}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<StudentDTO>> getStudentById(@PathVariable UUID uuid) {
        StudentDTO studentDTO = studentDirectoryService.getStudent(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(studentDTO, "Student retrieved successfully"));
    }

    /**
     * Retrieves a paginated list of students, each record projected to what the caller may see.
     *
     * @param pageable Pagination details.
     * @return A paginated list of student DTOs.
     */
    @Operation(summary = "Get all students", description = "Fetches a paginated list of students. Guardian contacts, demographic tag and audit fields appear only on the records the caller is related to.")
    @GetMapping
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<StudentDTO>>> getAllStudents(Pageable pageable) {
        Page<StudentDTO> students = studentDirectoryService.listStudents(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(PagedDTO.from(students, ServletUriComponentsBuilder.fromCurrentRequest().build().toString()), "Students retrieved successfully"));
    }

    /**
     * Updates an existing student by UUID.
     *
     * @param uuid       The UUID of the student to update.
     * @param studentDTO The updated student data.
     * @return The updated student DTO.
     */
    @Operation(summary = "Update a student", description = "Updates an existing student record. Restricted to the learner, an active guardian, a manager of one of the learner's organisations, or a platform admin; the record cannot be re-pointed at a different user account.", responses = {@ApiResponse(responseCode = "200", description = "Student updated successfully", content = @Content(schema = @Schema(implementation = StudentDTO.class))), @ApiResponse(responseCode = "403", description = "Caller is not related to this student, or is re-pointing the record"), @ApiResponse(responseCode = "404", description = "Student not found")})
    @PutMapping("/{uuid}")
    @PreAuthorize("@studentDirectorySecurityService.canUpdateStudent(#uuid, #studentDTO.userUuid())")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<StudentDTO>> updateStudent(@PathVariable UUID uuid, @Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO updatedStudent = studentService.updateStudent(uuid, studentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(updatedStudent, "Information updated successfully"));
    }

    /**
     * Deletes a student by UUID. Narrower than every other route here: the record is platform-wide,
     * and removing it strips the {@code student} domain along with the learner's standing at every
     * organisation they belong to.
     *
     * @param uuid The UUID of the student to delete.
     * @return A response entity with no content.
     */
    @Operation(summary = "Delete a student", description = "Removes a student record from the system. Restricted to the learner themselves or a platform admin, because deletion revokes the student domain platform-wide.", responses = {@ApiResponse(responseCode = "204", description = "Student deleted successfully"), @ApiResponse(responseCode = "403", description = "Caller is neither the learner nor a platform admin"), @ApiResponse(responseCode = "404", description = "Student not found")})
    @DeleteMapping("/{uuid}")
    @PreAuthorize("@studentDirectorySecurityService.canDeleteStudent(#uuid)")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Void>> deleteStudent(@PathVariable UUID uuid) {
        studentService.deleteStudent(uuid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(apps.sarafrika.elimika.shared.dto.ApiResponse.success(null, "Student deleted successfully"));
    }

    /**
     * Searches for students with pagination, each record projected to what the caller may see (see
     * {@link #getStudentById(UUID)}).
     *
     * @param searchParams Search parameters as key-value pairs.
     * @param pageable     Pagination details.
     * @return A paginated list of matching student DTOs.
     */
    @Operation(summary = "Search students", description = "Search for students based on criteria. Guardian contacts, demographic tag and audit fields appear only on the records the caller is related to.", responses = {@ApiResponse(responseCode = "200", description = "Search results returned successfully", content = @Content(schema = @Schema(implementation = Page.class)))})
    @GetMapping("/search")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<StudentDTO>>> searchStudents(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams, Pageable pageable) {
        Page<StudentDTO> students = studentDirectoryService.searchStudents(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(PagedDTO.from(students, ServletUriComponentsBuilder.fromCurrentRequest().build().toString()), "Search successful"));
    }
}
