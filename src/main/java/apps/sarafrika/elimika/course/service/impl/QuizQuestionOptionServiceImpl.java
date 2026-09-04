package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.QuizQuestionOptionDTO;
import apps.sarafrika.elimika.course.factory.QuizQuestionOptionFactory;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.service.QuizQuestionOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizQuestionOptionServiceImpl implements QuizQuestionOptionService {

    private final QuizQuestionOptionRepository quizQuestionOptionRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    private final GenericSpecificationBuilder<QuizQuestionOption> specificationBuilder;

    private static final String QUIZ_QUESTION_OPTION_NOT_FOUND_TEMPLATE = "Quiz question option with ID %s not found";
    private static final String QUIZ_QUESTION_NOT_FOUND_TEMPLATE = "Quiz question with ID %s not found";

    @Override
    public QuizQuestionOptionDTO createQuizQuestionOption(QuizQuestionOptionDTO quizQuestionOptionDTO) {
        QuizQuestionOption quizQuestionOption = QuizQuestionOptionFactory.toEntity(quizQuestionOptionDTO);

        // Set defaults
        if (quizQuestionOption.getIsCorrect() == null) {
            quizQuestionOption.setIsCorrect(false);
        }
        if (quizQuestionOption.getDisplayOrder() == null) {
            quizQuestionOption.setDisplayOrder(1);
        }

        QuizQuestionOption savedQuizQuestionOption = quizQuestionOptionRepository.save(quizQuestionOption);
        return QuizQuestionOptionFactory.toDTO(savedQuizQuestionOption);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizQuestionOptionDTO getQuizQuestionOptionByUuid(UUID uuid) {
        return quizQuestionOptionRepository.findByUuid(uuid)
                .map(QuizQuestionOptionFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_QUESTION_OPTION_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizQuestionOptionDTO> getAllQuizQuestionOptions(Pageable pageable) {
        specificationBuilder.validateSortProperties(QuizQuestionOption.class, pageable);
        return quizQuestionOptionRepository.findAll(pageable).map(QuizQuestionOptionFactory::toDTO);
    }

    @Override
    public QuizQuestionOptionDTO updateQuizQuestionOption(UUID uuid, QuizQuestionOptionDTO quizQuestionOptionDTO) {
        QuizQuestionOption existingQuizQuestionOption = quizQuestionOptionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_QUESTION_OPTION_NOT_FOUND_TEMPLATE, uuid)));

        updateQuizQuestionOptionFields(existingQuizQuestionOption, quizQuestionOptionDTO);

        QuizQuestionOption updatedQuizQuestionOption = quizQuestionOptionRepository.save(existingQuizQuestionOption);
        return QuizQuestionOptionFactory.toDTO(updatedQuizQuestionOption);
    }

    @Override
    public void deleteQuizQuestionOption(UUID uuid) {
        if (!quizQuestionOptionRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(QUIZ_QUESTION_OPTION_NOT_FOUND_TEMPLATE, uuid));
        }
        quizQuestionOptionRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizQuestionOptionDTO> search(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(QuizQuestionOption.class, pageable);
        Specification<QuizQuestionOption> spec = specificationBuilder.buildSpecification(
                QuizQuestionOption.class, searchParams);
        return quizQuestionOptionRepository.findAll(spec, pageable).map(QuizQuestionOptionFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizQuestionOptionDTO> getOptionsForQuestion(UUID quizUuid, UUID questionUuid, Pageable pageable) {
        requireQuestionInQuiz(quizUuid, questionUuid);
        Specification<QuizQuestionOption> byQuestion =
                (root, query, cb) -> cb.equal(root.get("questionUuid"), questionUuid);
        return quizQuestionOptionRepository.findAll(byQuestion, pageable).map(QuizQuestionOptionFactory::toDTO);
    }

    @Override
    public QuizQuestionOptionDTO addOptionToQuestion(UUID quizUuid, UUID questionUuid,
                                                     QuizQuestionOptionDTO optionDTO) {
        requireQuestionInQuiz(quizUuid, questionUuid);

        QuizQuestionOption option = QuizQuestionOptionFactory.toEntity(optionDTO);
        option.setQuestionUuid(questionUuid);
        if (option.getIsCorrect() == null) {
            option.setIsCorrect(false);
        }
        if (option.getDisplayOrder() == null) {
            option.setDisplayOrder(1);
        }
        return QuizQuestionOptionFactory.toDTO(quizQuestionOptionRepository.save(option));
    }

    @Override
    public QuizQuestionOptionDTO updateOptionInQuestion(UUID quizUuid, UUID questionUuid, UUID optionUuid,
                                                        QuizQuestionOptionDTO optionDTO) {
        QuizQuestionOption existing = requireOptionInQuestion(quizUuid, questionUuid, optionUuid);
        updateQuizQuestionOptionFields(existing, optionDTO);
        // The body may edit the option's text and marking, never move it to another question.
        existing.setQuestionUuid(questionUuid);
        return QuizQuestionOptionFactory.toDTO(quizQuestionOptionRepository.save(existing));
    }

    @Override
    public void deleteOptionFromQuestion(UUID quizUuid, UUID questionUuid, UUID optionUuid) {
        requireOptionInQuestion(quizUuid, questionUuid, optionUuid);
        quizQuestionOptionRepository.deleteByUuid(optionUuid);
    }

    /**
     * Proves the question sits under the quiz the caller was authorised against.
     */
    private QuizQuestion requireQuestionInQuiz(UUID quizUuid, UUID questionUuid) {
        QuizQuestion question = quizQuestionRepository.findByUuid(questionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_QUESTION_NOT_FOUND_TEMPLATE, questionUuid)));
        if (!quizUuid.equals(question.getQuizUuid())) {
            throw new AccessDeniedException("Quiz question does not belong to the requested quiz.");
        }
        return question;
    }

    /**
     * The second link of the same chain: the option must sit under that question.
     */
    private QuizQuestionOption requireOptionInQuestion(UUID quizUuid, UUID questionUuid, UUID optionUuid) {
        requireQuestionInQuiz(quizUuid, questionUuid);
        QuizQuestionOption option = quizQuestionOptionRepository.findByUuid(optionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_QUESTION_OPTION_NOT_FOUND_TEMPLATE, optionUuid)));
        if (!questionUuid.equals(option.getQuestionUuid())) {
            throw new AccessDeniedException("Quiz question option does not belong to the requested question.");
        }
        return option;
    }

    private void updateQuizQuestionOptionFields(QuizQuestionOption existingQuizQuestionOption, QuizQuestionOptionDTO dto) {
        if (dto.questionUuid() != null) {
            existingQuizQuestionOption.setQuestionUuid(dto.questionUuid());
        }
        if (dto.optionText() != null) {
            existingQuizQuestionOption.setOptionText(dto.optionText());
        }
        if (dto.isCorrect() != null) {
            existingQuizQuestionOption.setIsCorrect(dto.isCorrect());
        }
        if (dto.displayOrder() != null) {
            existingQuizQuestionOption.setDisplayOrder(dto.displayOrder());
        }
    }
}