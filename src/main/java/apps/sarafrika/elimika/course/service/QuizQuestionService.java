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