package apps.sarafrika.elimika.assessment.service;

import apps.sarafrika.elimika.assessment.dto.request.CreateQuestionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateQuestionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.QuestionResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface QuestionService {

    ResponsePageableDTO<QuestionResponseDTO> findQuestionsByAssessment(Long assessmentId, Pageable pageable);

    ResponseDTO<QuestionResponseDTO> findQuestion(Long assessmentId, Long id);

    ResponseDTO<QuestionResponseDTO> findQuestionById(Long id);

    ResponseDTO<Void> createQuestion(Long assessmentId, CreateQuestionRequestDTO createQuestionRequestDTO);

    ResponseDTO<Void> updateQuestion(Long assessmentId, UpdateQuestionRequestDTO updateQuestionRequestDTO, Long id);

    void deleteQuestion(Long assessmentId, Long id);
}
