package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface QuizService {
    QuizDTO createQuiz(QuizDTO quizDTO);

    QuizDTO getQuizByUuid(UUID uuid);

    Page<QuizDTO> getAllQuizzes(Pageable pageable);

    QuizDTO updateQuiz(UUID uuid, QuizDTO quizDTO);

    void deleteQuiz(UUID uuid);

    Page<QuizDTO> search(Map<String, String> searchParams, Pageable pageable);
}