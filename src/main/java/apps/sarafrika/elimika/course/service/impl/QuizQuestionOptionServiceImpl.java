package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.QuizQuestionOptionDTO;
import apps.sarafrika.elimika.course.factory.QuizQuestionOptionFactory;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.service.QuizQuestionOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizQuestionOptionServiceImpl implements QuizQuestionOptionService {

    private final QuizQuestionOptionRepository quizQuestionOptionRepository;

    private final GenericSpecificationBuilder<QuizQuestionOption> specificationBuilder;

    private static final String QUIZ_QUESTION_OPTION_NOT_FOUND_TEMPLATE = "Quiz question option with ID %s not found";

    @Override
    public QuizQuestionOptionDTO createQuizQuestionOption(QuizQuestionOptionDTO quizQuestionOptionDTO) {
        QuizQuestionOption quizQuestionOption = QuizQuestionOptionFactory.toEntity(quizQuestionOptionDTO);

        // Set defaults
        if (quizQuestionOption.getIsCorrect() == null) {
            quizQuestionOption.setIsCorrect(false);
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
        Specification<QuizQuestionOption> spec = specificationBuilder.buildSpecification(
                QuizQuestionOption.class, searchParams);
        return quizQuestionOptionRepository.findAll(spec, pageable).map(QuizQuestionOptionFactory::toDTO);
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