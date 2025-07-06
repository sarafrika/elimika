package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.service.*;
import io.swagger.v3.oas.annotations.Operation;
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

    private final QuizService quizService;
    private final QuizQuestionService quizQuestionService;
    private final QuizQuestionOptionService quizQuestionOptionService;
    private final QuizAttemptService quizAttemptService;

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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<QuizDTO>> createQuiz(
            @Valid @RequestBody QuizDTO quizDTO) {
        QuizDTO createdQuiz = quizService.createQuiz(quizDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<QuizDTO>> getQuizByUuid(
            @PathVariable UUID uuid) {
        QuizDTO quizDTO = quizService.getQuizByUuid(uuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(quizDTO, "Quiz retrieved successfully"));
    }

    @Operation(
            summary = "Get all quizzes",
            description = "Retrieves paginated list of all quizzes with filtering support."
    )
    @GetMapping
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<QuizDTO>>> getAllQuizzes(
            Pageable pageable) {
        Page<QuizDTO> quizzes = quizService.getAllQuizzes(pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<QuizDTO>> updateQuiz(
            @PathVariable UUID uuid,
            @Valid @RequestBody QuizDTO quizDTO) {
        QuizDTO updatedQuiz = quizService.updateQuiz(uuid, quizDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<QuizQuestionDTO>> addQuizQuestion(
            @PathVariable UUID quizUuid,
            @Valid @RequestBody QuizQuestionDTO questionDTO) {
        QuizQuestionDTO createdQuestion = quizQuestionService.createQuizQuestion(questionDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdQuestion, "Question added successfully"));
    }

    @Operation(
            summary = "Get quiz questions",
            description = "Retrieves all questions for a quiz in display order with computed properties."
    )
    @GetMapping("/{quizUuid}/questions")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<List<QuizQuestionDTO>>> getQuizQuestions(
            @PathVariable UUID quizUuid) {
        List<QuizQuestionDTO> questions = quizQuestionService.getQuestionsByQuiz(quizUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(questions, "Quiz questions retrieved successfully"));
    }

    @Operation(
            summary = "Update quiz question",
            description = "Updates a specific question within a quiz."
    )
    @PutMapping("/{quizUuid}/questions/{questionUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<QuizQuestionDTO>> updateQuizQuestion(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @Valid @RequestBody QuizQuestionDTO questionDTO) {
        QuizQuestionDTO updatedQuestion = quizQuestionService.updateQuizQuestion(questionUuid, questionDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedQuestion, "Question updated successfully"));
    }

    @Operation(
            summary = "Delete quiz question",
            description = "Removes a question from a quiz including all options and responses."
    )
    @DeleteMapping("/{quizUuid}/questions/{questionUuid}")
    public ResponseEntity<Void> deleteQuizQuestion(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid) {
        quizQuestionService.deleteQuizQuestion(questionUuid);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reorder quiz questions",
            description = "Updates the display order of questions within a quiz."
    )
    @PostMapping("/{quizUuid}/questions/reorder")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<String>> reorderQuizQuestions(
            @PathVariable UUID quizUuid,
            @RequestBody List<UUID> questionUuids) {
        quizQuestionService.reorderQuestions(quizUuid, questionUuids);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success("Questions reordered successfully", "Quiz question order updated"));
    }

    // ===== QUESTION OPTIONS =====

    @Operation(
            summary = "Add option to question",
            description = "Creates a new option for a multiple choice or true/false question."
    )
    @PostMapping("/{quizUuid}/questions/{questionUuid}/options")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<QuizQuestionOptionDTO>> addQuestionOption(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @Valid @RequestBody QuizQuestionOptionDTO optionDTO) {
        QuizQuestionOptionDTO createdOption = quizQuestionOptionService.createQuizQuestionOption(optionDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apps.sarafrika.elimika.common.dto.ApiResponse
                        .success(createdOption, "Option added successfully"));
    }

    @Operation(
            summary = "Get question options",
            description = "Retrieves all options for a specific question."
    )
    @GetMapping("/{quizUuid}/questions/{questionUuid}/options")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<QuizQuestionOptionDTO>>> getQuestionOptions(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("questionUuid", questionUuid.toString());
        Page<QuizQuestionOptionDTO> options = quizQuestionOptionService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(options, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Question options retrieved successfully"));
    }

    @Operation(
            summary = "Update question option",
            description = "Updates a specific option for a question."
    )
    @PutMapping("/{quizUuid}/questions/{questionUuid}/options/{optionUuid}")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<QuizQuestionOptionDTO>> updateQuestionOption(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @PathVariable UUID optionUuid,
            @Valid @RequestBody QuizQuestionOptionDTO optionDTO) {
        QuizQuestionOptionDTO updatedOption = quizQuestionOptionService.updateQuizQuestionOption(optionUuid, optionDTO);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(updatedOption, "Option updated successfully"));
    }

    @Operation(
            summary = "Delete question option",
            description = "Removes an option from a question."
    )
    @DeleteMapping("/{quizUuid}/questions/{questionUuid}/options/{optionUuid}")
    public ResponseEntity<Void> deleteQuestionOption(
            @PathVariable UUID quizUuid,
            @PathVariable UUID questionUuid,
            @PathVariable UUID optionUuid) {
        quizQuestionOptionService.deleteQuizQuestionOption(optionUuid);
        return ResponseEntity.noContent().build();
    }

    // ===== QUIZ ATTEMPTS =====

    @Operation(
            summary = "Get quiz attempts",
            description = "Retrieves all attempts for a specific quiz with scoring data."
    )
    @GetMapping("/{quizUuid}/attempts")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<QuizAttemptDTO>>> getQuizAttempts(
            @PathVariable UUID quizUuid,
            Pageable pageable) {
        Map<String, String> searchParams = Map.of("quizUuid", quizUuid.toString());
        Page<QuizAttemptDTO> attempts = quizAttemptService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<BigDecimal>> getQuizTotalPoints(
            @PathVariable UUID quizUuid) {
        BigDecimal totalPoints = quizQuestionService.getTotalQuizPoints(quizUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(totalPoints, "Quiz total points retrieved successfully"));
    }

    @Operation(
            summary = "Get question category distribution",
            description = "Returns distribution of question types within a quiz."
    )
    @GetMapping("/{quizUuid}/question-distribution")
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<Map<String, Long>>> getQuestionDistribution(
            @PathVariable UUID quizUuid) {
        Map<String, Long> distribution = quizQuestionService.getQuestionCategoryDistribution(quizUuid);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<QuizDTO>>> searchQuizzes(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<QuizDTO> quizzes = quizService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<QuizQuestionDTO>>> searchQuestions(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<QuizQuestionDTO> questions = quizQuestionService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
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
    public ResponseEntity<apps.sarafrika.elimika.common.dto.ApiResponse<PagedDTO<QuizAttemptDTO>>> searchAttempts(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {
        Page<QuizAttemptDTO> attempts = quizAttemptService.search(searchParams, pageable);
        return ResponseEntity.ok(apps.sarafrika.elimika.common.dto.ApiResponse
                .success(PagedDTO.from(attempts, ServletUriComponentsBuilder
                                .fromCurrentRequestUri().build().toString()),
                        "Attempt search completed successfully"));
    }
}