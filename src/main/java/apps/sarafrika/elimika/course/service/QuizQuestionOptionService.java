package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizQuestionOptionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface QuizQuestionOptionService {
    QuizQuestionOptionDTO createQuizQuestionOption(QuizQuestionOptionDTO quizQuestionOptionDTO);

    QuizQuestionOptionDTO getQuizQuestionOptionByUuid(UUID uuid);

    Page<QuizQuestionOptionDTO> getAllQuizQuestionOptions(Pageable pageable);

    QuizQuestionOptionDTO updateQuizQuestionOption(UUID uuid, QuizQuestionOptionDTO quizQuestionOptionDTO);

    void deleteQuizQuestionOption(UUID uuid);

    Page<QuizQuestionOptionDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * Options of one question of one quiz.
     * <p>
     * The quiz is part of the address, not decoration. Authorization for an answer key is granted
     * against the quiz — "is this quiz's course yours to mark" — so a lookup that reaches the
     * options by question alone lets a caller pair a quiz they may read with a question they may
     * not, and walk out with somebody else's {@code is_correct} flags. Every method here therefore
     * proves the whole chain quiz → question → option before it touches a row.
     *
     * @throws org.springframework.security.access.AccessDeniedException when the question is not in
     *                                                                   the quiz
     */
    Page<QuizQuestionOptionDTO> getOptionsForQuestion(UUID quizUuid, UUID questionUuid, Pageable pageable);

    /**
     * Adds an option to a question of a quiz. The question the option lands on comes from the path,
     * never from the body.
     */
    QuizQuestionOptionDTO addOptionToQuestion(UUID quizUuid, UUID questionUuid, QuizQuestionOptionDTO optionDTO);

    /**
     * Updates one option of one question of one quiz.
     */
    QuizQuestionOptionDTO updateOptionInQuestion(UUID quizUuid, UUID questionUuid, UUID optionUuid,
                                                 QuizQuestionOptionDTO optionDTO);

    /**
     * Deletes one option of one question of one quiz.
     */
    void deleteOptionFromQuestion(UUID quizUuid, UUID questionUuid, UUID optionUuid);
}