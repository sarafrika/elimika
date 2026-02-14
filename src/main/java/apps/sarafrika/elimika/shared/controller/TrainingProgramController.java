package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.service.*;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for comprehensive training program management.
 */
@RestController
@RequestMapping(TrainingProgramController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Training Program Management", description = "Complete program lifecycle management including courses, enrollments, and certifications")
public class TrainingProgramController {

    public static final String API_ROOT_PATH = "/api/v1/programs";

    private final TrainingProgramService trainingProgramService;
    private final ProgramCourseService programCourseService;
    private final ProgramEnrollmentService programEnrollmentService;
    private final ProgramRequirementService programRequirementService;
    private final CertificateService certificateService;
    private final ProgramTrainingApplicationService programTrainingApplicationService;

    // ===== PROGRAM BASIC OPERATIONS =====

    @Operation(
            summary = "Create a new training program",
            description = "Creates a new training program with default DRAFT status and inactive state.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Program created successfully",
                            content = @Content(schema = @Schema(implementation = TrainingProgramDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PreAuthorize("@domainSecurityService.isCourseCreator()")
    @PostMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<TrainingProgramDTO>> createTrainingProgram(
            @Valid @RequestBody TrainingProgramDTO programDTO) {
        TrainingProgramDTO createdProgram = trainingProgramService.createTrainingProgram(programDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdProgram, "Training program created successfully"));
    }

    @Operation(
            summary = "Get program by UUID",
            description = "Retrieves a complete program profile including computed properties and analytics.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Program found"),
                    @ApiResponse(responseCode = "404", description = "Program not found")
            }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<TrainingProgramDTO>> getTrainingProgramByUuid(
            @PathVariable UUID uuid) {
        TrainingProgramDTO programDTO = trainingProgramService.getTrainingProgramByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(programDTO, "Training program retrieved successfully"));
    }

    @Operation(
            summary = "Get all programs",
            description = "Retrieves paginated list of all training programs with filtering support."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<TrainingProgramDTO>>> getAllTrainingPrograms(
            Pageable pageable) {
        Page<TrainingProgramDTO> programs = trainingProgramService.getAllTrainingPrograms(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(programs, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Training programs retrieved successfully"));
    }

    @Operation(
            summary = "Update training program",
            description = "Updates an existing training program with selective field updates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Program updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Program not found")
            }
    )
    @PutMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<TrainingProgramDTO>> updateTrainingProgram(
            @PathVariable UUID uuid,
            @Valid @RequestBody TrainingProgramDTO programDTO) {
        TrainingProgramDTO updatedProgram = trainingProgramService.updateTrainingProgram(uuid, programDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedProgram, "Training program updated successfully"));
    }

    @Operation(
            summary = "Delete training program",
            description = "Permanently removes a training program and its associated data.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Program deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Program not found")
            }
    )
    @DeleteMapping("/{uuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Void>> deleteTrainingProgram(@PathVariable UUID uuid) {
        trainingProgramService.deleteTrainingProgram(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(null, "Program deleted successfully"));
    }

    @Operation(
            summary = "Publish training program",
            description = "Publishes a program making it available for enrollment.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Program published successfully"),
                    @ApiResponse(responseCode = "400", description = "Program not ready for publishing")
            }
    )
    @PostMapping("/{uuid}/publish")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<TrainingProgramDTO>> publishProgram(
            @PathVariable UUID uuid) {
        if (!trainingProgramService.isProgramReadyForPublishing(uuid)) {
            return ResponseEntity.badRequest()
                    .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                            .error("Program is not ready for publishing. Ensure it has title, description, and courses."));
        }

        TrainingProgramDTO publishedProgram = trainingProgramService.publishProgram(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(publishedProgram, "Training program published successfully"));
    }

    @Operation(
            summary = "Search training programs",
            description = """
                    Advanced program search with flexible criteria and operators.
                    
                    **Common Program Search Examples:**
                    - `title_like=data science` - Programs with titles containing "data science"
                    - `status=PUBLISHED` - Only published programs
                    - `active=true` - Only active programs
                    - `status_in=PUBLISHED,ACTIVE` - Published or active programs
                    - `price_lte=500.00` - Programs priced at $500 or less
                    - `price=null` - Free programs
                    - `courseCreatorUuid=uuid` - Programs by specific course creator
                    - `categoryUuid=uuid` - Programs in specific category
                    - `totalDurationHours_gte=40` - Programs 40+ hours long
                    - `totalDurationHours_between=20,100` - Programs between 20-100 hours
                    - `createdDate_gte=2024-01-01T00:00:00` - Programs created after Jan 1, 2024
                    
                    **Advanced Program Queries:**
                    - `status=PUBLISHED&active=true&price_lte=100` - Published, active programs under $100
                    - `title_like=certification&totalDurationHours_gte=50` - Certification programs 50+ hours
                    - `courseCreatorUuid=uuid&status=PUBLISHED` - Published programs by specific course creator
                    
                    For complete operator documentation, see the instructor search endpoint.
                    """
    )
    @GetMapping("/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<TrainingProgramDTO>>> searchTrainingPrograms(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<TrainingProgramDTO> programs = trainingProgramService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(programs, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Program search completed successfully"));
    }

    // ===== PROGRAM COURSES =====

    @Operation(
            summary = "Add course to program",
            description = "Associates a course with a program, setting sequence and requirement status."
    )
    @PostMapping("/{programUuid}/courses")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<ProgramCourseDTO>> addProgramCourse(
            @PathVariable UUID programUuid,
            @Valid @RequestBody ProgramCourseDTO programCourseDTO) {
        ProgramCourseDTO createdProgramCourse = programCourseService.createProgramCourse(programCourseDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdProgramCourse, "Course added to program successfully"));
    }

    @Operation(
            summary = "Get program courses",
            description = "Retrieves all courses in a program in sequence order with requirement status."
    )
    @GetMapping("/{programUuid}/courses")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseDTO>>> getProgramCourses(
            @PathVariable UUID programUuid) {
        List<CourseDTO> courses = trainingProgramService.getAllProgramCourses(programUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(courses, "Program courses retrieved successfully"));
    }

    @Operation(
            summary = "Get required courses",
            description = "Retrieves only the required courses for a program."
    )
    @GetMapping("/{programUuid}/courses/required")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseDTO>>> getRequiredCourses(
            @PathVariable UUID programUuid) {
        List<CourseDTO> requiredCourses = trainingProgramService.getRequiredCourses(programUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(requiredCourses, "Required courses retrieved successfully"));
    }

    @Operation(
            summary = "Get optional courses",
            description = "Retrieves only the optional courses for a program."
    )
    @GetMapping("/{programUuid}/courses/optional")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<CourseDTO>>> getOptionalCourses(
            @PathVariable UUID programUuid) {
        List<CourseDTO> optionalCourses = trainingProgramService.getOptionalCourses(programUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(optionalCourses, "Optional courses retrieved successfully"));
    }

    @Operation(
            summary = "Update program course",
            description = "Updates course association settings within a program."
    )
    @PutMapping("/{programUuid}/courses/{courseUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<ProgramCourseDTO>> updateProgramCourse(
            @PathVariable UUID programUuid,
            @PathVariable UUID courseUuid,
            @Valid @RequestBody ProgramCourseDTO programCourseDTO) {
        ProgramCourseDTO updatedProgramCourse = programCourseService.updateProgramCourse(courseUuid, programCourseDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedProgramCourse, "Program course updated successfully"));
    }

    @Operation(
            summary = "Remove course from program",
            description = "Removes the association between a course and program."
    )
    @DeleteMapping("/{programUuid}/courses/{courseUuid}")
    public ResponseEntity<Void> removeProgramCourse(
            @PathVariable UUID programUuid,
            @PathVariable UUID courseUuid) {
        programCourseService.deleteProgramCourse(courseUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== PROGRAM ENROLLMENTS =====

    @Operation(
            summary = "Get program enrollments",
            description = "Retrieves enrollment data for a specific program with completion analytics."
    )
    @GetMapping("/{programUuid}/enrollments")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<ProgramEnrollmentDTO>>> getProgramEnrollments(
            @PathVariable UUID programUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("programUuid", programUuid.toString());
        Page<ProgramEnrollmentDTO> enrollments = programEnrollmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(enrollments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Program enrollments retrieved successfully"));
    }

    @Operation(
            summary = "Get program completion rate",
            description = "Returns the completion rate percentage for a program."
    )
    @GetMapping("/{programUuid}/completion-rate")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Double>> getProgramCompletionRate(
            @PathVariable UUID programUuid) {
        double completionRate = trainingProgramService.getProgramCompletionRate(programUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(completionRate, "Program completion rate retrieved successfully"));
    }

    // ===== PROGRAM REQUIREMENTS =====

    @Operation(
            summary = "Add requirement to program",
            description = "Adds a new requirement or prerequisite to a program."
    )
    @PostMapping("/{programUuid}/requirements")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<ProgramRequirementDTO>> addProgramRequirement(
            @PathVariable UUID programUuid,
            @Valid @RequestBody ProgramRequirementDTO requirementDTO) {
        ProgramRequirementDTO createdRequirement = programRequirementService.createProgramRequirement(requirementDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdRequirement, "Program requirement added successfully"));
    }

    @Operation(
            summary = "Get program requirements",
            description = "Retrieves all requirements for a specific program."
    )
    @GetMapping("/{programUuid}/requirements")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<ProgramRequirementDTO>>> getProgramRequirements(
            @PathVariable UUID programUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("programUuid", programUuid.toString());
        Page<ProgramRequirementDTO> requirements = programRequirementService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(requirements, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Program requirements retrieved successfully"));
    }

    @Operation(
            summary = "Update program requirement",
            description = "Updates a specific requirement for a program."
    )
    @PutMapping("/{programUuid}/requirements/{requirementUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<ProgramRequirementDTO>> updateProgramRequirement(
            @PathVariable UUID programUuid,
            @PathVariable UUID requirementUuid,
            @Valid @RequestBody ProgramRequirementDTO requirementDTO) {
        ProgramRequirementDTO updatedRequirement = programRequirementService.updateProgramRequirement(requirementUuid, requirementDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedRequirement, "Program requirement updated successfully"));
    }

    @Operation(
            summary = "Delete program requirement",
            description = "Removes a requirement from a program."
    )
    @DeleteMapping("/{programUuid}/requirements/{requirementUuid}")
    public ResponseEntity<Void> deleteProgramRequirement(
            @PathVariable UUID programUuid,
            @PathVariable UUID requirementUuid) {
        programRequirementService.deleteProgramRequirement(requirementUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== PROGRAM CERTIFICATES =====

    @Operation(
            summary = "Get program certificates",
            description = "Retrieves all certificates issued for program completions."
    )
    @GetMapping("/{programUuid}/certificates")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<CertificateDTO>>> getProgramCertificates(
            @PathVariable UUID programUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("programUuid", programUuid.toString());
        Page<CertificateDTO> certificates = certificateService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(certificates, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Program certificates retrieved successfully"));
    }

    // ===== PROGRAM ANALYTICS =====

    @Operation(
            summary = "Get active programs",
            description = "Retrieves all currently active and published programs."
    )
    @GetMapping("/active")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<TrainingProgramDTO>>> getActivePrograms(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("active", "true", "admin_approved", "true");
        Page<TrainingProgramDTO> activePrograms = trainingProgramService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(activePrograms, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Active programs retrieved successfully"));
    }

    @Operation(
            summary = "Get published programs",
            description = "Retrieves all published programs available for enrollment."
    )
    @GetMapping("/published")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<TrainingProgramDTO>>> getPublishedPrograms(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("status", "PUBLISHED", "admin_approved", "true");
        Page<TrainingProgramDTO> publishedPrograms = trainingProgramService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(publishedPrograms, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Published programs retrieved successfully"));
    }

    @Operation(
            summary = "Get free programs",
            description = "Retrieves all programs available at no cost."
    )
    @GetMapping("/free")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<TrainingProgramDTO>>> getFreePrograms(
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("price", "null");
        Page<TrainingProgramDTO> freePrograms = trainingProgramService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(freePrograms, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Free programs retrieved successfully"));
    }

    @Operation(
            summary = "Get programs by course creator",
            description = "Retrieves all programs created by a specific course creator."
    )
    @GetMapping("/creator/{courseCreatorUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<TrainingProgramDTO>>> getProgramsByCourseCreator(
            @PathVariable UUID courseCreatorUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("courseCreatorUuid", courseCreatorUuid.toString());
        Page<TrainingProgramDTO> courseCreatorPrograms = trainingProgramService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(courseCreatorPrograms, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Course creator programs retrieved successfully"));
    }

    @Operation(
            summary = "Get programs by category",
            description = "Retrieves all programs in a specific category."
    )
    @GetMapping("/category/{categoryUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<TrainingProgramDTO>>> getProgramsByCategory(
            @PathVariable UUID categoryUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("categoryUuid", categoryUuid.toString());
        Page<TrainingProgramDTO> categoryPrograms = trainingProgramService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(categoryPrograms, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Category programs retrieved successfully"));
    }

    // ===== SEARCH ENDPOINTS FOR PROGRAM ENTITIES =====

    @Operation(
            summary = "Search program courses",
            description = """
                    Search course associations within programs.
                    
                    **Common Program Course Search Examples:**
                    - `programUuid=uuid` - All courses for specific program
                    - `courseUuid=uuid` - All programs containing specific course
                    - `isRequired=true` - Only required course associations
                    - `sequenceOrder_gte=3` - Courses from sequence 3 onwards
                    """
    )
    @GetMapping("/courses/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<ProgramCourseDTO>>> searchProgramCourses(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<ProgramCourseDTO> programCourses = programCourseService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(programCourses, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Program courses search completed successfully"));
    }

    @Operation(
            summary = "Search program enrollments",
            description = """
                    Search enrollment records across all programs.
                    
                    **Common Program Enrollment Search Examples:**
                    - `programUuid=uuid` - All enrollments for specific program
                    - `studentUuid=uuid` - All program enrollments for specific student
                    - `status=COMPLETED` - Only completed program enrollments
                    - `progressPercentage_gte=90` - Students with 90%+ program progress
                    - `enrollmentDate_gte=2024-01-01T00:00:00` - Program enrollments from 2024
                    - `finalGrade_gte=85` - Program completions with grade 85+
                    """
    )
    @GetMapping("/enrollments/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<ProgramEnrollmentDTO>>> searchProgramEnrollments(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<ProgramEnrollmentDTO> enrollments = programEnrollmentService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(enrollments, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Program enrollments search completed successfully"));
    }

    @Operation(
            summary = "Search program requirements",
            description = """
                    Search program requirements and prerequisites.
                    
                    **Common Program Requirement Search Examples:**
                    - `programUuid=uuid` - All requirements for specific program
                    - `requirementType=PREREQUISITE` - Only prerequisites
                    - `isMandatory=true` - Only mandatory requirements
                    - `requirementText_like=certification` - Requirements mentioning "certification"
                    """
    )
    @GetMapping("/requirements/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<ProgramRequirementDTO>>> searchProgramRequirements(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<ProgramRequirementDTO> requirements = programRequirementService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(requirements, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Program requirements search completed successfully"));
    }
    // ===== PROGRAM TRAINING APPLICATIONS =====

    @Operation(
            summary = "Submit program training application",
            description = """
                    Allows an instructor or organisation to apply for permission to deliver the specified training program.

                    **Application Workflow:**
                    - Applicants submit once per program. Rejected applications can be resubmitted, which reopens the request.
                    - Duplicate pending or approved submissions are rejected with clear error messages. Revoked applicants must resubmit to regain access.
                    - Program creators review applications using the approval endpoints below.
                    """
    )
    @PostMapping("/{programUuid}/training-applications")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<ProgramTrainingApplicationDTO>> submitProgramTrainingApplication(
            @PathVariable UUID programUuid,
            @Valid @RequestBody ProgramTrainingApplicationRequest request) {
        ProgramTrainingApplicationDTO application = programTrainingApplicationService.submitApplication(programUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(application, "Training application submitted successfully"));
    }

    @Operation(
            summary = "List program training applications",
            description = """
                    Retrieves applications for a program. Optionally filter by status using `status=pending|approved|rejected|revoked`.
                    """
    )
    @GetMapping("/{programUuid}/training-applications")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<ProgramTrainingApplicationDTO>>> listProgramTrainingApplications(
            @PathVariable UUID programUuid,
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {

        Optional<CourseTrainingApplicationStatus> statusFilter = Optional.ofNullable(status)
                .filter(s -> !s.isBlank())
                .map(CourseTrainingApplicationStatus::fromValue);

        Map<String, String> filters = new HashMap<>();
        filters.put("programUuid", programUuid.toString());
        statusFilter.ifPresent(applicationStatus -> filters.put("status", applicationStatus.getValue()));

        Page<ProgramTrainingApplicationDTO> applications = programTrainingApplicationService.search(filters, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(applications, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Training applications retrieved successfully"));
    }

    @Operation(
            summary = "Search program training applications",
            description = """
                    Advanced search for training applications using flexible operators on any DTO field.
                    Supports filters such as `status`, `applicantType`, `programUuid`, `applicantUuid`,
                    `createdDate_between`, and more.
                    """
    )
    @GetMapping("/training-applications/search")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<ProgramTrainingApplicationDTO>>> searchProgramTrainingApplications(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {

        Page<ProgramTrainingApplicationDTO> applications = programTrainingApplicationService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(applications, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Training application search completed successfully"));
    }

    @Operation(
            summary = "Get program training application",
            description = "Retrieves a specific training application for a program."
    )
    @GetMapping("/{programUuid}/training-applications/{applicationUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<ProgramTrainingApplicationDTO>> getProgramTrainingApplication(
            @PathVariable UUID programUuid,
            @PathVariable UUID applicationUuid) {
        ProgramTrainingApplicationDTO application = programTrainingApplicationService.getApplication(programUuid, applicationUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(application, "Training application retrieved successfully"));
    }

    @Operation(
            summary = "Decide on program training application",
            description = """
                    Applies a decision to an instructor or organisation application to deliver the training program.
                    Use the `action` query parameter with values `approve`, `reject`, or `revoke`.
                    """
    )
    @PostMapping("/{programUuid}/training-applications/{applicationUuid}")
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<ProgramTrainingApplicationDTO>> decideOnProgramTrainingApplication(
            @PathVariable UUID programUuid,
            @PathVariable UUID applicationUuid,
            @RequestParam("action") String action,
            @Valid @RequestBody(required = false) ProgramTrainingApplicationDecisionRequest decisionRequest) {

        ProgramTrainingApplicationDecisionRequest payload =
                decisionRequest != null ? decisionRequest : new ProgramTrainingApplicationDecisionRequest(null);

        ProgramTrainingApplicationDTO application = switch (action.toLowerCase()) {
            case "approve" -> programTrainingApplicationService.approveApplication(programUuid, applicationUuid, payload);
            case "reject" -> programTrainingApplicationService.rejectApplication(programUuid, applicationUuid, payload);
            case "revoke" -> programTrainingApplicationService.revokeApplication(programUuid, applicationUuid, payload);
            default -> throw new IllegalArgumentException("Unsupported action '" + action + "'. Allowed values: approve, reject, revoke.");
        };

        String message = switch (action.toLowerCase()) {
            case "approve" -> "Training application approved successfully";
            case "reject" -> "Training application rejected successfully";
            case "revoke" -> "Training access revoked successfully";
            default -> "Training application updated";
        };

        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse.success(application, message));
    }
}
