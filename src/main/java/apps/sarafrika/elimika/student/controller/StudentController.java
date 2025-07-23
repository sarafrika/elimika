package apps.sarafrika.elimika.student.controller;

import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.student.dto.StudentDTO;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(StudentController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "Endpoints for managing students")
public class StudentController {
    public static final String API_ROOT_PATH = "/api/v1/students";

    private final StudentService studentService;

    /**
     * Creates a new student.
     *
     * @param studentDTO The student data to be created.
     * @return The created student DTO.
     */
    @Operation(summary = "Create a new student", description = "Saves a new student record in the system.", responses = {@ApiResponse(responseCode = "201", description = "Student created successfully", content = @Content(schema = @Schema(implementation = StudentDTO.class))), @ApiResponse(responseCode = "400", description = "Invalid request data")})
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<StudentDTO>> createStudent(@Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO createdStudent = studentService.createStudent(studentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(apps.sarafrika.elimika.common.dto.ApiResponse.success(createdStudent, "Student created successfully"));
    }


    /**
     * Retrieves a student by UUID.
     *
     * @param uuid The UUID of the student.
     * @return The student DTO if found.
     */
    @Operation(summary = "Get student by ID", description = "Fetches a student by their UUID.", responses = {@ApiResponse(responseCode = "200", description = "Student found", content = @Content(schema = @Schema(implementation = StudentDTO.class))), @ApiResponse(responseCode = "404", description = "Student not found")})
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<StudentDTO>> getStudentById(@PathVariable UUID uuid) {
        StudentDTO studentDTO = studentService.getStudentByUuId(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(studentDTO, "Student retrieved successfully"));
    }

    /**
     * Retrieves a paginated list of all students.
     *
     * @param pageable Pagination details.
     * @return A paginated list of student DTOs.
     */
    @Operation(summary = "Get all students", description = "Fetches a paginated list of students.")
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<StudentDTO>>> getAllStudents(Pageable pageable) {
        Page<StudentDTO> students = studentService.getAllStudents(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(PagedDTO.from(students, ServletUriComponentsBuilder.fromCurrentRequest().build().toString()), "Students retrieved successfully"));
    }

    /**
     * Updates an existing student by UUID.
     *
     * @param uuid       The UUID of the student to update.
     * @param studentDTO The updated student data.
     * @return The updated student DTO.
     */
    @Operation(summary = "Update a student", description = "Updates an existing student record.", responses = {@ApiResponse(responseCode = "200", description = "Student updated successfully", content = @Content(schema = @Schema(implementation = StudentDTO.class))), @ApiResponse(responseCode = "404", description = "Student not found")})
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<StudentDTO>> updateStudent(@PathVariable UUID uuid, @Valid @RequestBody StudentDTO studentDTO) {
        StudentDTO updatedStudent = studentService.updateStudent(uuid, studentDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(updatedStudent, "Information updated successfully"));
    }

    /**
     * Deletes a student by UUID.
     *
     * @param uuid The UUID of the student to delete.
     * @return A response entity with no content.
     */
    @Operation(summary = "Delete a student", description = "Removes a student record from the system.", responses = {@ApiResponse(responseCode = "204", description = "Student deleted successfully"), @ApiResponse(responseCode = "404", description = "Student not found")})
    @DeleteMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<Void>> deleteStudent(@PathVariable UUID uuid) {
        studentService.deleteStudent(uuid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(apps.sarafrika.elimika.common.dto.ApiResponse.success(null, "Student deleted successfully"));
    }

    /**
     * Searches for students with pagination.
     *
     * @param searchParams Search parameters as key-value pairs.
     * @param pageable     Pagination details.
     * @return A paginated list of matching student DTOs.
     */
    @Operation(summary = "Search students", description = "Search for students based on criteria.", responses = {@ApiResponse(responseCode = "200", description = "Search results returned successfully", content = @Content(schema = @Schema(implementation = Page.class)))})
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<StudentDTO>>> searchStudents(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams, Pageable pageable) {
        Page<StudentDTO> students = studentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse.success(PagedDTO.from(students, ServletUriComponentsBuilder.fromCurrentRequest().build().toString()), "Search successful"));
    }
}
