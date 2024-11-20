package apps.sarafrika.elimika.assessment.service;

import apps.sarafrika.elimika.assessment.dto.request.CreateAnswerOptionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateAnswerOptionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.AnswerOptionResponseDTO;
import apps.sarafrika.elimika.assessment.dto.response.QuestionResponseDTO;

import java.util.List;

public interface AnswerOptionService {

    List<AnswerOptionResponseDTO> findAnswerOptionsForQuestion(Long questionId);

    AnswerOptionResponseDTO findAnswerOption(Long id);

    void createAnswerOptions(QuestionResponseDTO question, List<CreateAnswerOptionRequestDTO> createAnswerOptionRequestDTOS);

    void updateAnswerOption(Long id, UpdateAnswerOptionRequestDTO updateAnswerOptionRequestDTO);

    void deleteAnswerOption(Long id);

    void deleteAllAnswerOptionsForQuestion(Long questionId);
}
