package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for comprehensive quiz and assessment management.
 */
@RestController
@RequestMapping(QuizController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Quiz Management", description = "Complete quiz lifecycle including questions, attempts, and analytics")
public class QuizController {

    public static final String API_ROOT_PATH = "/api/v1/quizzes";
    private static final String USER_DOMAIN = "T(apps.sarafrika.elimika.shared.utils.enums.UserDomain)";
    private static final String MANAGEMENT_ACCESS = "@domainSecurityService.hasAnyDomain("
            + USER_DOMAIN + ".course_creator, " + USER_DOMAIN + ".instructor, " + USER_DOMAIN + ".admin)";
    private static final String STUDENT_QUIZ_ACCESS = "@domainSecurityService.hasAnyDomain("
            + USER_DOMAIN + ".student, " + USER_DOMAIN + ".course_creator, "
            + USER_DOMAIN + ".instructor, " + USER_DOMAIN + ".admin)";
    /**
     * Quiz metadata a learner enrolled in the quiz's course may read. {@code QuizDTO} carries no
     * questions or options, so no answer key travels with it — those stay on the management-only
     * question endpoints and the student-view flow.
     */
    private static final String LEARNER_QUIZ_READ = "@learnerContentAccess.canReadQuiz(#quizUuid)";
    private static final String LEARNER_QUIZ_READ_BY_UUID = "@learnerContentAccess.canReadQuiz(#uuid)";
    /**
     * The inside of one quiz — its questions and, on their options, the {@code is_correct} answer
     * key — belongs to the course that owns the quiz.
     * <p>
     * {@link #MANAGEMENT_ACCESS} only asked whether the caller held a teaching domain
     * <em>somewhere</em> on the platform, so any instructor of any course could read any other
     * course's answer keys and rewrite its questions. This asks the question that was meant: is
     * this quiz's course yours to mark, as its owner, as an instructor approved to train it, or
     * through an organisation approved to train it.
     */
    private static final String QUIZ_MANAGEMENT = "@courseSecurityService.canManageQuiz(#quizUuid) "
            + "or @domainSecurityService.isPlatformAdmin()";
    /** The same decision for routes that name the quiz {@code uuid} rather than {@code quizUuid}. */
    private static final String QUIZ_MANAGEMENT_BY_UUID = "@courseSecurityService.canManageQuiz(#uuid) "
            + "or @domainSecurityService.isPlatformAdmin()";
    /**
     * Attempts, and the sittings that produce them, belong to the course's teaching staff plus the
     * learners on the course itself. The learner half only opens the endpoint: {@code
     * LearnerAssessmentScope} still narrows the rows a learner receives to their own enrolments, so
     * a classmate's marks never travel.
     * <p>
     * The learner half asks for an enrolment in any state rather than a currently-open one, because
     * these routes cover a learner reading back their own past work: dropping a course does not
     * un-sit the quizzes taken while on it.
     */
    private static final String QUIZ_RESULTS_ACCESS = QUIZ_MANAGEMENT
            + " or @courseSecurityService.hasCourseEnrollmentForQuiz(#quizUuid)";

    private final QuizService quizService;
    private final QuizQuestionService quizQuestionService;
    private final QuizQuestionOptionService quizQuestionOptionService;
    private final QuizAttemptService quizAttemptService;
    private final StudentQuizViewService studentQuizViewService;
    private final StudentQuizSubmissionService studentQuizSubmissionService;
    private final QuizGradingService quizGradingService;
    private final DomainSecurityService domainSecurityService;

    @Operation(
            summary = "Get student-safe quiz view",
            description = "Retrieves a quiz payload for students without configured answer keys."
    )
    @GetMapping("/{quizUuid}/student-view")
    @PreAuthorize(QUIZ_RESULTS_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<StudentQuizDTO>> getStudentQuizView(
            @PathVariable UUID quizUuid,
            @RequestParam(name = "enrollment_uuid", required = false) UUID enrollmentUuid) {
        StudentQuizDTO quiz = studentQuizViewService.getStudentQuiz(quizUuid, enrollmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(quiz, "Student quiz retrieved successfully"));
    }

    @Operation(
            summary = "Get graded student quiz review",
            description = "Retrieves a student's graded quiz review, including correct answers after grading."
    )
    @GetMapping("/{quizUuid}/attempts/{attemptUuid}/review")
    @PreAuthorize(QUIZ_RESULTS_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<StudentQuizReviewDTO>> getStudentQuizReview(
            @PathVariable UUID quizUuid,
            @PathVariable UUID attemptUuid,
            @RequestParam(name = "enrollment_uuid", required = false) UUID enrollmentUuid) {
        StudentQuizReviewDTO review = studentQuizViewService.getStudentQuizReview(quizUuid, attemptUuid, enrollmentUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(review, "Student quiz review retrieved successfully"));
    }

    // ===== STUDENT QUIZ TAKING =====

    @Operation(
            summary = "Start a quiz attempt",
            description = "Starts a new quiz attempt for the student's enrollment, or resumes an in-progress "
                    + "attempt. Enforces the quiz's attempts-allowed limit.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Attempt started or resumed"),
                    @ApiResponse(responseCode = "409", description = "No remaining attempts allowed")
            }
    )
    @PostMapping("/{quizUuid}/attempts")
    @PreAuthorize(QUIZ_RESULTS_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizAttemptDTO>> startQuizAttempt(
            @PathVariable UUID quizUuid,
            @RequestParam(name = "enrollment_uuid", required = false) UUID enrollmentUuid) {
        QuizAttemptDTO attempt = studentQuizSubmissionService.startAttempt(quizUuid, enrollmentUuid);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(attempt, "Quiz attempt started successfully"));
    }

    @Operation(
            summary = "Save quiz answers",
            description = "Upserts the student's answers onto an in-progress attempt. Can be called repeatedly "
                    + "to autosave progress before submitting."
    )
    @PutMapping("/{quizUuid}/attempts/{attemptUuid}/responses")
    @PreAuthorize(QUIZ_RESULTS_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizAttemptDTO>> saveQuizResponses(
            @PathVariable UUID quizUuid,
            @PathVariable UUID attemptUuid,
            @RequestParam(name = "enrollment_uuid", required = false) UUID enrollmentUuid,
            @Valid @RequestBody List<QuizResponseSubmissionDTO> responses) {
        QuizAttemptDTO attempt = studentQuizSubmissionService.saveResponses(
                quizUuid, attemptUuid, enrollmentUuid, responses);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(attempt, "Quiz answers saved successfully"));
    }

    @Operation(
            summary = "Submit a quiz attempt",
            description = "Submits the attempt and grades it. Objective questions are auto-graded immediately; "
                    + "attempts containing text questions remain pending instructor grading.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Attempt submitted and graded"),
                    @ApiResponse(responseCode = "409", description = "Attempt already submitted")
            }
    )
    @PostMapping("/{quizUuid}/attempts/{attemptUuid}/submit")
    @PreAuthorize(QUIZ_RESULTS_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizAttemptDTO>> submitQuizAttempt(
            @PathVariable UUID quizUuid,
            @PathVariable UUID attemptUuid,
            @RequestParam(name = "enrollment_uuid", required = false) UUID enrollmentUuid,
            @Valid @RequestBody(required = false) QuizAttemptSubmissionRequest request) {
        QuizAttemptDTO attempt = studentQuizSubmissionService.submitAttempt(
                quizUuid, attemptUuid, enrollmentUuid, request);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(attempt, "Quiz attempt submitted successfully"));
    }

    @Operation(
            summary = "Grade a quiz text response",
            description = "Records an instructor grade for a short-answer or essay response on a submitted "
                    + "attempt. When every answered text question is graded, the attempt is finalised and "
                    + "its grade synced to the gradebook."
    )
    @PostMapping("/{quizUuid}/attempts/{attemptUuid}/questions/{questionUuid}/grade")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizAttemptDTO>> gradeQuizTextResponse(
            @PathVariable UUID quizUuid,
            @PathVariable UUID attemptUuid,
            @PathVariable UUID questionUuid,
            @Valid @RequestBody QuizManualGradeRequest request) {
        QuizAttemptDTO attempt = quizGradingService.gradeTextResponse(
                attemptUuid, questionUuid, request.points(), request.isCorrect(), request.feedback(),
                domainSecurityService.getCurrentUserUuid());
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(attempt, "Quiz response graded successfully"));
    }

    @Operation(
            summary = "Create a new quiz",
            description = "Creates a new quiz with default DRAFT status and inactive state.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Quiz created successfully",
                            content = @Content(schema = @Schema(implementation = QuizDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping
    @PreAuthorize(MANAGEMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizDTO>> createQuiz(
            @Valid @RequestBody QuizDTO quizDTO) {
        QuizDTO createdQuiz = quizService.createQuiz(quizDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdQuiz, "Quiz created successfully"));
    }

    @Operation(
            summary = "Get quiz by UUID",
            description = "Retrieves a complete quiz including questions and computed properties.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Quiz found"),
                    @ApiResponse(responseCode = "404", description = "Quiz not found")
            }
    )
    @GetMapping("/{uuid}")
    @PreAuthorize(LEARNER_QUIZ_READ_BY_UUID)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizDTO>> getQuizByUuid(
            @PathVariable UUID uuid) {
        QuizDTO quizDTO = quizService.getQuizByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(quizDTO, "Quiz retrieved successfully"));
    }

    @Operation(
            summary = "Get all quizzes",
            description = "Retrieves paginated list of all quizzes with filtering support."
    )
    @GetMapping
    @PreAuthorize(MANAGEMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<QuizDTO>>> getAllQuizzes(
            Pageable pageable) {
        Page<QuizDTO> quizzes = quizService.getAllQuizzes(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(quizzes, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Quizzes retrieved successfully"));
    }

    @Operation(
            summary = "Update quiz",
            description = "Updates an existing quiz with selective field updates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Quiz updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Quiz not found")
            }
    )
    @PutMapping("/{uuid}")
    @PreAuthorize(QUIZ_MANAGEMENT_BY_UUID)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizDTO>> updateQuiz(
            @PathVariable UUID uuid,
            @Valid @RequestBody QuizDTO quizDTO) {
        QuizDTO updatedQuiz = quizService.updateQuiz(uuid, quizDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedQuiz, "Quiz updated successfully"));
    }

    @Operation(
            summary = "Delete quiz",
            description = "Permanently removes a quiz and all associated data.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Quiz deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Quiz not found")
            }
    )
    @DeleteMapping("/{uuid}")
    @PreAuthorize(QUIZ_MANAGEMENT_BY_UUID)
    public ResponseEntity<Void> deleteQuiz(@PathVariable UUID uuid) {
        quizService.deleteQuiz(uuid);
        return ResponseEntity.noContent().build();
    }

    // ===== QUIZ QUESTIONS =====

    @Operation(
            summary = "Add question to quiz",
            description = "Creates a new question for the specified quiz with automatic ordering."
    )
    @PostMapping("/{quizUuid}/questions")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizQuestionDTO>> addQuizQuestion(
            @PathVariable UUID quizUuid,
            @Valid @RequestBody QuizQuestionDTO questionDTO) {
        QuizQuestionDTO createdQuestion = quizQuestionService.addQuestionToQuiz(quizUuid, questionDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdQuestion, "Question added successfully"));
    }

    @Operation(
            summary = "Get quiz questions",
            description = "Retrieves all questions for a quiz in display order with computed properties."
    )
    @GetMapping("/{quizUuid}/questions")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<List<QuizQuestionDTO>>> getQuizQuestions(
            @PathVariable UUID quizUuid) {
        List<QuizQuestionDTO> questions = quizQuestionService.getQuestionsByQuiz(quizUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(questions, "Quiz questions retrieved successfully"));
    }

    @Operation(
            summary = "Update quiz question",
            description = "Updates a specific question within a quiz."
    )
    @PutMapping("/{quizUuid}/questions/{questionUuid}")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizQuestionDTO>> updateQuizQuestion(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @Valid @RequestBody QuizQuestionDTO questionDTO) {
        QuizQuestionDTO updatedQuestion = quizQuestionService.updateQuestionInQuiz(quizUuid, questionUuid, questionDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedQuestion, "Question updated successfully"));
    }

    @Operation(
            summary = "Delete quiz question",
            description = "Removes a question from a quiz including all options and responses."
    )
    @DeleteMapping("/{quizUuid}/questions/{questionUuid}")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<Void> deleteQuizQuestion(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid) {
        quizQuestionService.deleteQuestionFromQuiz(quizUuid, questionUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reorder quiz questions",
            description = "Updates the display order of questions within a quiz."
    )
    @PostMapping("/{quizUuid}/questions/reorder")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<String>> reorderQuizQuestions(
            @PathVariable UUID quizUuid,
            @RequestBody List<UUID> questionUuids) {
        quizQuestionService.reorderQuestions(quizUuid, questionUuids);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success("Questions reordered successfully", "Quiz question order updated"));
    }

    // ===== QUESTION OPTIONS =====

    @Operation(
            summary = "Add option to question",
            description = "Creates a new option for a multiple choice or true/false question."
    )
    @PostMapping("/{quizUuid}/questions/{questionUuid}/options")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizQuestionOptionDTO>> addQuestionOption(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @Valid @RequestBody QuizQuestionOptionDTO optionDTO) {
        QuizQuestionOptionDTO createdOption = quizQuestionOptionService.addOptionToQuestion(
                quizUuid, questionUuid, optionDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.shared.dto.ApiResponse
                        .success(createdOption, "Option added successfully"));
    }

    @Operation(
            summary = "Get question options",
            description = "Retrieves all options for a specific question."
    )
    @GetMapping("/{quizUuid}/questions/{questionUuid}/options")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<QuizQuestionOptionDTO>>> getQuestionOptions(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            Pageable pageable) {
        Page<QuizQuestionOptionDTO> options = quizQuestionOptionService.getOptionsForQuestion(
                quizUuid, questionUuid, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(options, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Question options retrieved successfully"));
    }

    @Operation(
            summary = "Update question option",
            description = "Updates a specific option for a question."
    )
    @PutMapping("/{quizUuid}/questions/{questionUuid}/options/{optionUuid}")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<QuizQuestionOptionDTO>> updateQuestionOption(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @PathVariable UUID optionUuid,
            @Valid @RequestBody QuizQuestionOptionDTO optionDTO) {
        QuizQuestionOptionDTO updatedOption = quizQuestionOptionService.updateOptionInQuestion(
                quizUuid, questionUuid, optionUuid, optionDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(updatedOption, "Option updated successfully"));
    }

    @Operation(
            summary = "Delete question option",
            description = "Removes an option from a question."
    )
    @DeleteMapping("/{quizUuid}/questions/{questionUuid}/options/{optionUuid}")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<Void> deleteQuestionOption(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @PathVariable UUID optionUuid) {
        quizQuestionOptionService.deleteOptionFromQuestion(quizUuid, questionUuid, optionUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== QUIZ ATTEMPTS =====

    @Operation(
            summary = "Get quiz attempts",
            description = "Retrieves attempts for a specific quiz with scoring data. Teaching staff see "
                    + "every learner's attempts; students see only attempts on their own enrolments."
    )
    @GetMapping("/{quizUuid}/attempts")
    @PreAuthorize(QUIZ_RESULTS_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<QuizAttemptDTO>>> getQuizAttempts(
            @PathVariable UUID quizUuid,
            Pageable pageable) {
        Page<QuizAttemptDTO> attempts = quizAttemptService.getAttemptsForQuiz(quizUuid, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(attempts, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Quiz attempts retrieved successfully"));
    }

    // ===== QUIZ ANALYTICS =====

    @Operation(
            summary = "Get quiz total points",
            description = "Returns the maximum possible points for a quiz."
    )
    @GetMapping("/{quizUuid}/total-points")
    @PreAuthorize(LEARNER_QUIZ_READ)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<BigDecimal>> getQuizTotalPoints(
            @PathVariable UUID quizUuid) {
        BigDecimal totalPoints = quizQuestionService.getTotalQuizPoints(quizUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(totalPoints, "Quiz total points retrieved successfully"));
    }

    @Operation(
            summary = "Get question category distribution",
            description = "Returns distribution of question types within a quiz."
    )
    @GetMapping("/{quizUuid}/question-distribution")
    @PreAuthorize(QUIZ_MANAGEMENT)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<Map<String, Long>>> getQuestionDistribution(
            @PathVariable UUID quizUuid) {
        Map<String, Long> distribution = quizQuestionService.getQuestionCategoryDistribution(quizUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(distribution, "Question distribution retrieved successfully"));
    }

    // ===== SEARCH ENDPOINTS =====

    @Operation(
            summary = "Search quizzes",
            description = """
                    Advanced quiz search with flexible criteria and operators.
                    
                    **Common Quiz Search Examples:**
                    - `title_like=midterm` - Quizzes with "midterm" in title
                    - `lessonUuid=uuid` - Quizzes for specific lesson
                    - `status=PUBLISHED` - Only published quizzes
                    - `active=true` - Only active quizzes
                    - `timeLimitMinutes_gte=30` - Quizzes with 30+ minute time limit
                    - `attemptsAllowed_lte=3` - Quizzes with 3 or fewer attempts allowed
                    - `passingScore_gte=70` - Quizzes with passing score 70%+
                    """
    )
    @GetMapping("/search")
    @PreAuthorize(STUDENT_QUIZ_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<QuizDTO>>> searchQuizzes(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<QuizDTO> quizzes = quizService.searchForCaller(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(quizzes, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Quiz search completed successfully"));
    }

    @Operation(
            summary = "Search quiz questions",
            description = """
                    Search questions across all quizzes.
                    
                    **Common Question Search Examples:**
                    - `quizUuid=uuid` - All questions for specific quiz
                    - `questionType=MULTIPLE_CHOICE` - Only multiple choice questions
                    - `points_gte=2` - Questions worth 2+ points
                    - `questionText_like=calculate` - Questions containing "calculate"
                    """
    )
    @GetMapping("/questions/search")
    @PreAuthorize(MANAGEMENT_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<QuizQuestionDTO>>> searchQuestions(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<QuizQuestionDTO> questions = quizQuestionService.searchForCaller(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(questions, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Question search completed successfully"));
    }

    @Operation(
            summary = "Search quiz attempts",
            description = """
                    Search quiz attempts across all quizzes.
                    
                    **Common Attempt Search Examples:**
                    - `quizUuid=uuid` - All attempts for specific quiz
                    - `enrollmentUuid=uuid` - All attempts by specific student
                    - `status=COMPLETED` - Only completed attempts
                    - `isPassed=true` - Only passing attempts
                    - `percentage_gte=85` - Attempts with 85%+ score
                    - `startedAt_gte=2024-01-01T00:00:00` - Attempts from 2024
                    """
    )
    @GetMapping("/attempts/search")
    @PreAuthorize(STUDENT_QUIZ_ACCESS)
    public ResponseEntity<apps.sarafrika.elimika.shared.dto.ApiResponse<PagedDTO<QuizAttemptDTO>>> searchAttempts(
            @Parameter(
                    description = "Optional search parameters for filtering",
                    schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                    explode = Explode.TRUE
            )
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<QuizAttemptDTO> attempts = quizAttemptService.searchForCaller(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.shared.dto.ApiResponse
                .success(PagedDTO.from(attempts, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Attempt search completed successfully"));
    }
}
