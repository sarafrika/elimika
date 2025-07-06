package apps.sarafrika.elimika.course.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface QuizResponseService {
    QuizResponseDTO createQuizResponse(QuizResponseDTO quizResponseDTO);

    QuizResponseDTO getQuizResponseByUuid(UUID uuid);

    Page<QuizResponseDTO> getAllQuizResponses(Pageable pageable);

    QuizResponseDTO updateQuizResponse(UUID uuid, QuizResponseDTO quizResponseDTO);

    void deleteQuizResponse(UUID uuid);

    Page<QuizResponseDTO> search(Map<String, String> searchParams, Pageable pageable);
}