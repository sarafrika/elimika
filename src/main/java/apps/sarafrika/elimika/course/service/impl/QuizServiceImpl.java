package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.QuizDTO;
import apps.sarafrika.elimika.course.factory.QuizFactory;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.service.QuizService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final GenericSpecificationBuilder<Quiz> specificationBuilder;
    private final LessonRepository lessonRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    private static final String QUIZ_NOT_FOUND_TEMPLATE = "Quiz with ID %s not found";

    @Override
    public QuizDTO createQuiz(QuizDTO quizDTO) {
        Quiz quiz = QuizFactory.toEntity(quizDTO);

        // Set defaults based on QuizDTO business logic
        if (quiz.getStatus() == null) {
            quiz.setStatus(ContentStatus.DRAFT);
        }
        if (quiz.getActive() == null) {
            quiz.setActive(false);
        }

        Quiz savedQuiz = quizRepository.save(quiz);
        return QuizFactory.toDTO(savedQuiz);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizDTO getQuizByUuid(UUID uuid) {
        return quizRepository.findByUuid(uuid)
                .map(QuizFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizDTO> getAllQuizzes(Pageable pageable) {
        return quizRepository.findAll(pageable).map(QuizFactory::toDTO);
    }

    @Override
    public QuizDTO updateQuiz(UUID uuid, QuizDTO quizDTO) {
        Quiz existingQuiz = quizRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_NOT_FOUND_TEMPLATE, uuid)));

        updateQuizFields(existingQuiz, quizDTO);

        Quiz updatedQuiz = quizRepository.save(existingQuiz);
        return QuizFactory.toDTO(updatedQuiz);
    }

    @Override
    public void deleteQuiz(UUID uuid) {
        if (!quizRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(QUIZ_NOT_FOUND_TEMPLATE, uuid));
        }
        quizRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Map<String, String> effectiveParams = new HashMap<>(searchParams);

        if (effectiveParams.containsKey("courseUuid")) {
            UUID courseUuid = UUID.fromString(effectiveParams.remove("courseUuid"));
            List<UUID> lessonUuids = lessonRepository.findByCourseUuid(courseUuid)
                    .stream()
                    .map(Lesson::getUuid)
                    .filter(Objects::nonNull)
                    .toList();

            if (lessonUuids.isEmpty()) {
                return Page.empty(pageable);
            }

            String lessonUuidFilter = lessonUuids.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));
            effectiveParams.put("lessonUuid_in", lessonUuidFilter);
        }

        Specification<Quiz> spec = specificationBuilder.buildSpecification(
                Quiz.class, effectiveParams);
        return quizRepository.findAll(spec, pageable).map(QuizFactory::toDTO);
    }

    private void updateQuizFields(Quiz existingQuiz, QuizDTO dto) {
        if (dto.lessonUuid() != null) {
            existingQuiz.setLessonUuid(dto.lessonUuid());
        }
        if (dto.title() != null) {
            existingQuiz.setTitle(dto.title());
        }
        if (dto.description() != null) {
            existingQuiz.setDescription(dto.description());
        }
        if (dto.instructions() != null) {
            existingQuiz.setInstructions(dto.instructions());
        }
        if (dto.timeLimitMinutes() != null) {
            existingQuiz.setTimeLimitMinutes(dto.timeLimitMinutes());
        }
        if (dto.attemptsAllowed() != null) {
            existingQuiz.setAttemptsAllowed(dto.attemptsAllowed());
        }
        if (dto.passingScore() != null) {
            existingQuiz.setPassingScore(dto.passingScore());
        }
        if (dto.rubricUuid() != null) {
            existingQuiz.setRubricUuid(dto.rubricUuid());
        }
        if (dto.status() != null) {
            existingQuiz.setStatus(dto.status());
        }
        if (dto.active() != null) {
            existingQuiz.setActive(dto.active());
        }
    }
}
