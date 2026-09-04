package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizQuestionDTO;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface QuizQuestionService {

    // Basic CRUD operations
    QuizQuestionDTO createQuizQuestion(QuizQuestionDTO quizQuestionDTO);

    QuizQuestionDTO getQuizQuestionByUuid(UUID uuid);

    Page<QuizQuestionDTO> getAllQuizQuestions(Pageable pageable);

    QuizQuestionDTO updateQuizQuestion(UUID uuid, QuizQuestionDTO quizQuestionDTO);

    void deleteQuizQuestion(UUID uuid);

    Page<QuizQuestionDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Searches questions, narrowed to the quizzes the caller actually marks.
     * <p>
     * The unrestricted {@link #search(Map, Pageable)} answers across every quiz on the platform, so
     * exposing it to a teaching role hands one course's staff another course's question bank. This
     * asks the same question the per-quiz routes ask — is this quiz's course yours — of every row.
     */
    Page<QuizQuestionDTO> searchForCaller(Map<String, String> searchParams, Pageable pageable);

    /**
     * Adds a question to a quiz. The quiz it lands on comes from the path, never from the body, so
     * a caller authorised against one quiz cannot plant a question in another.
     */
    QuizQuestionDTO addQuestionToQuiz(UUID quizUuid, QuizQuestionDTO questionDTO);

    /**
     * Updates one question of one quiz, refusing a question that belongs to a different quiz.
     * Authorization is granted against the quiz, so the question must be proven to sit under it.
     *
     * @throws org.springframework.security.access.AccessDeniedException when the question is not in
     *                                                                   the quiz
     */
    QuizQuestionDTO updateQuestionInQuiz(UUID quizUuid, UUID questionUuid, QuizQuestionDTO questionDTO);

    /**
     * Deletes one question of one quiz, refusing a question that belongs to a different quiz.
     */
    void deleteQuestionFromQuiz(UUID quizUuid, UUID questionUuid);

    // Domain-specific methods (missing from your interface)
    List<QuizQuestionDTO> getQuestionsByQuiz(UUID quizUuid);

    List<QuizQuestionDTO> getQuestionsByType(UUID quizUuid, QuestionType questionType);

    List<QuizQuestionDTO> getQuestionsByType(UUID quizUuid, String questionType);

    List<QuizQuestionDTO> getQuestionsRequiringOptions(UUID quizUuid);

    List<QuizQuestionDTO> getMultipleChoiceQuestions(UUID quizUuid);

    List<QuizQuestionDTO> getTrueFalseQuestions(UUID quizUuid);

    List<QuizQuestionDTO> getEssayQuestions(UUID quizUuid);

    List<QuizQuestionDTO> getShortAnswerQuestions(UUID quizUuid);

    // Analytics methods
    Map<String, Long> getQuestionCategoryDistribution(UUID quizUuid);

    BigDecimal getTotalQuizPoints(UUID quizUuid);

    double getAverageQuestionPoints(UUID quizUuid);

    // Question management methods
    void reorderQuestions(UUID quizUuid, List<UUID> questionUuids);

    int getNextDisplayOrder(UUID quizUuid);

    // Validation methods
    boolean hasQuestions(UUID quizUuid);

    boolean hasOptionsBasedQuestions(UUID quizUuid);

    boolean canDeleteQuestion(UUID questionUuid);
}