package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizQuestionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface QuizQuestionService {
    QuizQuestionDTO createQuizQuestion(QuizQuestionDTO quizQuestionDTO);

    QuizQuestionDTO getQuizQuestionByUuid(UUID uuid);

    Page<QuizQuestionDTO> getAllQuizQuestions(Pageable pageable);

    QuizQuestionDTO updateQuizQuestion(UUID uuid, QuizQuestionDTO quizQuestionDTO);

    void deleteQuizQuestion(UUID uuid);

    Page<QuizQuestionDTO> search(Map<String, String> searchParams, Pageable pageable);
}
