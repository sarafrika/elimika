package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface QuizAttemptService {
    QuizAttemptDTO createQuizAttempt(QuizAttemptDTO quizAttemptDTO);

    QuizAttemptDTO getQuizAttemptByUuid(UUID uuid);

    Page<QuizAttemptDTO> getAllQuizAttempts(Pageable pageable);

    QuizAttemptDTO updateQuizAttempt(UUID uuid, QuizAttemptDTO quizAttemptDTO);

    void deleteQuizAttempt(UUID uuid);

    Page<QuizAttemptDTO> search(Map<String, String> searchParams, Pageable pageable);
}