package apps.sarafrika.elimika.assessment.service.impl;

import apps.sarafrika.elimika.assessment.config.exceptions.QuestionNotFoundException;
import apps.sarafrika.elimika.assessment.config.exceptions.QuestionValidationException;
import apps.sarafrika.elimika.assessment.dto.request.CreateQuestionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.request.UpdateQuestionRequestDTO;
import apps.sarafrika.elimika.assessment.dto.response.AnswerOptionResponseDTO;
import apps.sarafrika.elimika.assessment.dto.response.AssessmentResponseDTO;
import apps.sarafrika.elimika.assessment.dto.response.QuestionResponseDTO;
import apps.sarafrika.elimika.assessment.persistence.Question;
import apps.sarafrika.elimika.assessment.persistence.QuestionFactory;
import apps.sarafrika.elimika.assessment.persistence.QuestionRepository;
import apps.sarafrika.elimika.assessment.service.AnswerOptionService;
import apps.sarafrika.elimika.assessment.service.AssessmentService;
import apps.sarafrika.elimika.assessment.service.QuestionService;
import apps.sarafrika.elimika.assessment.util.enums.QuestionType;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final String ERROR_QUESTION_NOT_FOUND = "Question not found.";
    private static final String ERROR_ANSWER_OPTIONS_IS_NULL = "Answer options should not be null.";
    private static final String QUESTION_FOUND_SUCCESS = "Question retrieved successfully.";
    private static final String QUESTION_CREATED_SUCCESS = "Question persisted successfully.";
    private static final String QUESTION_UPDATED_SUCCESS = "Question updated successfully.";

    private final AssessmentService assessmentService;
    private final QuestionRepository questionRepository;
    private final AnswerOptionService answerOptionService;

    /*
     * TODO: Fetch answer options for questions.
     */
    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<QuestionResponseDTO> findQuestionsByAssessment(Long assessmentId, Pageable pageable) {
        Page<QuestionResponseDTO> questions = questionRepository.findAllByAssessmentId(assessmentId, pageable).stream()
                .map(question -> {

                    List<AnswerOptionResponseDTO> answerOptions = answerOptionService.findAnswerOptionsForQuestion(question.getId());

                    return QuestionResponseDTO.from(question, answerOptions);
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(questions.getContent(), questions.getNumber(), questions.getSize(),
                questions.getTotalPages(), questions.getTotalElements(), HttpStatus.OK.value(), null);

    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<QuestionResponseDTO> findQuestion(Long assessmentId, Long id) {
        final Question question = findQuestionByIdAndAssessmentId(id, assessmentId);

        List<AnswerOptionResponseDTO> answerOptions = answerOptionService.findAnswerOptionsForQuestion(id);

        QuestionResponseDTO questionResponseDTO = QuestionResponseDTO.from(question, answerOptions);

        return new ResponseDTO<>(questionResponseDTO, HttpStatus.OK.value(), QUESTION_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<QuestionResponseDTO> findQuestionById(Long id) {

        final Question question = questionRepository.findById(id).orElseThrow(() -> new QuestionNotFoundException(ERROR_QUESTION_NOT_FOUND));

        List<AnswerOptionResponseDTO> answerOptions = answerOptionService.findAnswerOptionsForQuestion(id);

        QuestionResponseDTO questionResponseDTO = QuestionResponseDTO.from(question, answerOptions);

        return new ResponseDTO<>(questionResponseDTO, HttpStatus.OK.value(), QUESTION_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private Question findQuestionByIdAndAssessmentId(Long id, Long assessmentId) {

        return questionRepository.findByIdAndAssessmentId(id, assessmentId).orElseThrow(() -> new QuestionNotFoundException(ERROR_QUESTION_NOT_FOUND));
    }

    @Transactional
    @Override
    public ResponseDTO<Void> createQuestion(Long assessmentId, CreateQuestionRequestDTO createQuestionRequestDTO) {

        ResponseDTO<AssessmentResponseDTO> assessment = assessmentService.findAssessment(assessmentId);

        Question question = QuestionFactory.create(createQuestionRequestDTO);

        question.setAssessmentId(assessment.data().id());

        Question savedQuestion = questionRepository.save(question);

        boolean isMultipleChoiceQuestion = savedQuestion.getQuestionType().equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE.name());
        boolean isTrueFalseQuestion = savedQuestion.getQuestionType().equalsIgnoreCase(QuestionType.TRUE_FALSE.name());

        if (isMultipleChoiceQuestion || isTrueFalseQuestion) {

            if (createQuestionRequestDTO.answerOptions() == null || createQuestionRequestDTO.answerOptions().isEmpty()) {
                throw new QuestionValidationException(ERROR_ANSWER_OPTIONS_IS_NULL);
            }

            answerOptionService.createAnswerOptions(QuestionResponseDTO.from(savedQuestion, null), createQuestionRequestDTO.answerOptions());
        }

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), QUESTION_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updateQuestion(Long assessmentId, UpdateQuestionRequestDTO updateQuestionRequestDTO, Long id) {

        Question question = findQuestionByIdAndAssessmentId(id, assessmentId);

        QuestionFactory.update(question, updateQuestionRequestDTO);

        Question updatedQuestion = questionRepository.save(question);

        boolean isMultipleChoiceQuestion = updatedQuestion.getQuestionType().equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE.name());
        boolean isTrueFalseQuestion = updatedQuestion.getQuestionType().equalsIgnoreCase(QuestionType.TRUE_FALSE.name());

        if (isMultipleChoiceQuestion || isTrueFalseQuestion) {

            if (updateQuestionRequestDTO.answerOptions() == null || updateQuestionRequestDTO.answerOptions().isEmpty()) {
                throw new QuestionValidationException(ERROR_ANSWER_OPTIONS_IS_NULL);
            }

            answerOptionService.deleteAllAnswerOptionsForQuestion(updatedQuestion.getId());

            /*
             * TODO: Implement update answer options
             */
        } else {

            answerOptionService.deleteAllAnswerOptionsForQuestion(updatedQuestion.getId());
        }

        return new ResponseDTO<>(null, HttpStatus.OK.value(), QUESTION_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteQuestion(Long assessmentId, Long id) {

        Question question = findQuestionByIdAndAssessmentId(id, assessmentId);

        questionRepository.delete(question);
    }
}