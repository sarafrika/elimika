package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import apps.sarafrika.elimika.course.factory.QuizAttemptFactory;
import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizResponse;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.QuizResponseRepository;
import apps.sarafrika.elimika.course.service.QuizAttemptService;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizAttemptServiceImpl implements QuizAttemptService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final GenericSpecificationBuilder<QuizAttempt> specificationBuilder;

    private final QuizRepository quizRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    private static final String ATTEMPT_NOT_FOUND_TEMPLATE = "Quiz attempt with ID %s not found";

    @Override
    public QuizAttemptDTO createQuizAttempt(QuizAttemptDTO quizAttemptDTO) {
        QuizAttempt attempt = QuizAttemptFactory.toEntity(quizAttemptDTO);

        // Set defaults based on QuizAttemptDTO business logic
        if (attempt.getStatus() == null) {
            attempt.setStatus(AttemptStatus.IN_PROGRESS);
        }
        if (attempt.getStartedAt() == null) {
            attempt.setStartedAt(LocalDateTime.now());
        }

        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);
        return QuizAttemptFactory.toDTO(savedAttempt);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizAttemptDTO getQuizAttemptByUuid(UUID uuid) {
        return quizAttemptRepository.findByUuid(uuid)
                .map(QuizAttemptFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ATTEMPT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizAttemptDTO> getAllQuizAttempts(Pageable pageable) {
        return quizAttemptRepository.findAll(pageable).map(QuizAttemptFactory::toDTO);
    }

    @Override
    public QuizAttemptDTO updateQuizAttempt(UUID uuid, QuizAttemptDTO quizAttemptDTO) {
        QuizAttempt existingAttempt = quizAttemptRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ATTEMPT_NOT_FOUND_TEMPLATE, uuid)));

        updateAttemptFields(existingAttempt, quizAttemptDTO);

        QuizAttempt updatedAttempt = quizAttemptRepository.save(existingAttempt);
        return QuizAttemptFactory.toDTO(updatedAttempt);
    }

    @Override
    public void deleteQuizAttempt(UUID uuid) {
        if (!quizAttemptRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(ATTEMPT_NOT_FOUND_TEMPLATE, uuid));
        }
        quizAttemptRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizAttemptDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<QuizAttempt> spec = specificationBuilder.buildSpecification(
                QuizAttempt.class, searchParams);
        return quizAttemptRepository.findAll(spec, pageable).map(QuizAttemptFactory::toDTO);
    }

    private BigDecimal calculateTotalScore(UUID attemptUuid) {
        // Calculate total score from quiz responses
        return quizResponseRepository.findByAttemptUuid(attemptUuid)
                .stream()
                .map(QuizResponse::getPointsEarned)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getMaxPossibleScore(UUID quizUuid) {
        // Get maximum possible score for the quiz
        return quizQuestionRepository.findByQuizUuid(quizUuid)
                .stream()
                .map(QuizQuestion::getPoints)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updateAttemptFields(QuizAttempt existingAttempt, QuizAttemptDTO dto) {
        if (dto.enrollmentUuid() != null) {
            existingAttempt.setEnrollmentUuid(dto.enrollmentUuid());
        }
        if (dto.quizUuid() != null) {
            existingAttempt.setQuizUuid(dto.quizUuid());
        }
        if (dto.attemptNumber() != null) {
            existingAttempt.setAttemptNumber(dto.attemptNumber());
        }
        if (dto.startedAt() != null) {
            existingAttempt.setStartedAt(dto.startedAt());
        }
        if (dto.submittedAt() != null) {
            existingAttempt.setSubmittedAt(dto.submittedAt());
        }
        if (dto.timeTakenMinutes() != null) {
            existingAttempt.setTimeTakenMinutes(dto.timeTakenMinutes());
        }
        if (dto.score() != null) {
            existingAttempt.setScore(dto.score());
        }
        if (dto.maxScore() != null) {
            existingAttempt.setMaxScore(dto.maxScore());
        }
        if (dto.percentage() != null) {
            existingAttempt.setPercentage(dto.percentage());
        }
        if (dto.isPassed() != null) {
            existingAttempt.setIsPassed(dto.isPassed());
        }
        if (dto.status() != null) {
            existingAttempt.setStatus(dto.status());
        }
    }
}