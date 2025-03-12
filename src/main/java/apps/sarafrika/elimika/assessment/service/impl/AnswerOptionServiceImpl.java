package apps.sarafrika.elimika.assessment.service.impl;

import apps.sarafrika.elimika.assessment.config.exceptions.AnswerOptionNotFoundException;
import apps.sarafrika.elimika.assessment.dto.request.CreateAnswerOptionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateAnswerOptionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.AnswerOptionResponseDTO;
import apps.sarafrika.elimika.assessment.dto.response.QuestionResponseDTO;
import apps.sarafrika.elimika.assessment.persistence.AnswerOption;
import apps.sarafrika.elimika.assessment.persistence.AnswerOptionFactory;
import apps.sarafrika.elimika.assessment.persistence.AnswerOptionRepository;
import apps.sarafrika.elimika.assessment.service.AnswerOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerOptionServiceImpl implements AnswerOptionService {

    private static final String ANSWER_OPTION_NOT_FOUND = "Answer Option not found.";

    private final AnswerOptionRepository answerOptionRepository;

    @Override
    public List<AnswerOptionResponseDTO> findAnswerOptionsForQuestion(Long questionId) {

        return answerOptionRepository.findAllByQuestionId(questionId).stream()
                .map(AnswerOptionResponseDTO::from)
                .toList();
    }

    @Override
    public AnswerOptionResponseDTO findAnswerOption(Long id) {

        AnswerOption answerOption = findAnswerOptionById(id);

        return AnswerOptionResponseDTO.from(answerOption);
    }

    private AnswerOption findAnswerOptionById(Long id) {

        return answerOptionRepository.findById(id).orElseThrow(() -> new AnswerOptionNotFoundException(ANSWER_OPTION_NOT_FOUND));
    }

    @Override
    public void createAnswerOptions(QuestionResponseDTO question, List<CreateAnswerOptionRequestDTO> createAnswerOptionRequestDTOS) {

        List<AnswerOption> answerOptions = createAnswerOptionRequestDTOS.stream()
                .map(createAnswerOptionRequestDTO -> {

                    AnswerOption answerOption = AnswerOptionFactory.create(createAnswerOptionRequestDTO);

                    answerOption.setQuestionId(question.id());

                    return answerOption;
                })
                .toList();


        answerOptionRepository.saveAll(answerOptions);
    }

    @Override
    public void updateAnswerOption(Long id, UpdateAnswerOptionRequestDTO updateAnswerOptionRequestDTO) {

    }

    @Override
    public void deleteAnswerOption(Long id) {

        AnswerOption answerOption = findAnswerOptionById(id);

        answerOptionRepository.delete(answerOption);
    }

    @Override
    public void deleteAllAnswerOptionsForQuestion(Long questionId) {

    }

}
