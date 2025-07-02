package apps.sarafrika.elimika.course.service;

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
}