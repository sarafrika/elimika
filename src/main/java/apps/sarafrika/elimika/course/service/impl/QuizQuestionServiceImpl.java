package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.QuizQuestionDTO;
import apps.sarafrika.elimika.course.factory.QuizQuestionFactory;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizResponseRepository;
import apps.sarafrika.elimika.course.service.QuizQuestionService;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizQuestionServiceImpl implements QuizQuestionService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final CourseSecuritySpi courseSecurityService;
    private final DomainSecurityService domainSecurityService;

    private final GenericSpecificationBuilder<QuizQuestion> specificationBuilder;

    private static final String QUIZ_QUESTION_NOT_FOUND_TEMPLATE = "Quiz question with ID %s not found";

    @Override
    public QuizQuestionDTO createQuizQuestion(QuizQuestionDTO quizQuestionDTO) {
        QuizQuestion quizQuestion = QuizQuestionFactory.toEntity(quizQuestionDTO);

        // Set defaults
        if (quizQuestion.getPoints() == null) {
            quizQuestion.setPoints(new BigDecimal("1.00"));
        }

        QuizQuestion savedQuizQuestion = quizQuestionRepository.save(quizQuestion);
        return QuizQuestionFactory.toDTO(savedQuizQuestion);
    }

    @Override
    @Transactional(readOnly = true)
    public QuizQuestionDTO getQuizQuestionByUuid(UUID uuid) {
        return quizQuestionRepository.findByUuid(uuid)
                .map(QuizQuestionFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_QUESTION_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizQuestionDTO> getAllQuizQuestions(Pageable pageable) {
        specificationBuilder.validateSortProperties(QuizQuestion.class, pageable);
        return quizQuestionRepository.findAll(pageable).map(QuizQuestionFactory::toDTO);
    }

    @Override
    public QuizQuestionDTO updateQuizQuestion(UUID uuid, QuizQuestionDTO quizQuestionDTO) {
        QuizQuestion existingQuizQuestion = quizQuestionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_QUESTION_NOT_FOUND_TEMPLATE, uuid)));

        updateQuizQuestionFields(existingQuizQuestion, quizQuestionDTO);

        QuizQuestion updatedQuizQuestion = quizQuestionRepository.save(existingQuizQuestion);
        return QuizQuestionFactory.toDTO(updatedQuizQuestion);
    }

    @Override
    public void deleteQuizQuestion(UUID uuid) {
        if (!quizQuestionRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(QUIZ_QUESTION_NOT_FOUND_TEMPLATE, uuid));
        }
        quizQuestionRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizQuestionDTO> search(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(QuizQuestion.class, pageable);
        Specification<QuizQuestion> spec = specificationBuilder.buildSpecification(
                QuizQuestion.class, searchParams);
        return quizQuestionRepository.findAll(spec, pageable).map(QuizQuestionFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuizQuestionDTO> searchForCaller(Map<String, String> searchParams, Pageable pageable) {
        Specification<QuizQuestion> spec = specificationBuilder.buildSpecification(
                QuizQuestion.class, searchParams);
        return quizQuestionRepository.findAll(restrictToMarkedQuizzes(spec), pageable)
                .map(QuizQuestionFactory::toDTO);
    }

    /**
     * Keeps a question search inside the courses the caller marks.
     * <p>
     * A question hangs off a quiz, a quiz off a lesson and a lesson off a course, and none of those
     * hops is a JPA association, so the restriction is a subquery over the pair rather than a join
     * path. Only a platform admin searches unnarrowed; a caller who marks nothing matches nothing,
     * which is the safe direction to fail in.
     */
    private Specification<QuizQuestion> restrictToMarkedQuizzes(Specification<QuizQuestion> base) {
        if (domainSecurityService.isPlatformAdmin()) {
            return base;
        }

        Set<UUID> markedCourses = courseSecurityService.manageableCourseUuids();
        Specification<QuizQuestion> mine = markedCourses.isEmpty()
                ? (root, query, cb) -> cb.disjunction()
                : (root, query, cb) -> {
                    Subquery<UUID> quizzes = query.subquery(UUID.class);
                    Root<Quiz> quiz = quizzes.from(Quiz.class);
                    Root<Lesson> lesson = quizzes.from(Lesson.class);
                    quizzes.select(quiz.get("uuid"))
                            .where(cb.and(
                                    cb.equal(lesson.get("uuid"), quiz.get("lessonUuid")),
                                    lesson.get("courseUuid").in(markedCourses)));
                    return root.get("quizUuid").in(quizzes);
                };

        return base == null ? mine : base.and(mine);
    }

    @Override
    public QuizQuestionDTO addQuestionToQuiz(UUID quizUuid, QuizQuestionDTO questionDTO) {
        QuizQuestion question = QuizQuestionFactory.toEntity(questionDTO);
        question.setQuizUuid(quizUuid);
        if (question.getPoints() == null) {
            question.setPoints(new BigDecimal("1.00"));
        }
        return QuizQuestionFactory.toDTO(quizQuestionRepository.save(question));
    }

    @Override
    public QuizQuestionDTO updateQuestionInQuiz(UUID quizUuid, UUID questionUuid, QuizQuestionDTO questionDTO) {
        QuizQuestion existing = requireQuestionInQuiz(quizUuid, questionUuid);
        updateQuizQuestionFields(existing, questionDTO);
        // The body may edit the question, never move it to another quiz.
        existing.setQuizUuid(quizUuid);
        return QuizQuestionFactory.toDTO(quizQuestionRepository.save(existing));
    }

    @Override
    public void deleteQuestionFromQuiz(UUID quizUuid, UUID questionUuid) {
        requireQuestionInQuiz(quizUuid, questionUuid);
        quizQuestionRepository.deleteByUuid(questionUuid);
    }

    /**
     * Proves the question sits under the quiz the caller was authorised against. Without this the
     * quiz in the path is decoration: the guard reads it, the handler ignores it, and a caller who
     * legitimately owns one quiz can rewrite the questions of any other.
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

    // Domain-specific methods leveraging QuizQuestionDTO computed properties
    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getQuestionsByQuiz(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(quizUuid)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getQuestionsByType(UUID quizUuid, QuestionType questionType) {
        return quizQuestionRepository.findByQuizUuidAndQuestionType(quizUuid, questionType)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .collect(Collectors.toList());
    }

    // Using computed properties for filtering
    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getQuestionsRequiringOptions(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuid(quizUuid)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .filter(QuizQuestionDTO::requiresOptions) // Using computed property
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getMultipleChoiceQuestions(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuidAndQuestionType(quizUuid, QuestionType.MULTIPLE_CHOICE)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getTrueFalseQuestions(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuidAndQuestionType(quizUuid, QuestionType.TRUE_FALSE)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getEssayQuestions(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuidAndQuestionType(quizUuid, QuestionType.ESSAY)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getShortAnswerQuestions(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuidAndQuestionType(quizUuid, QuestionType.SHORT_ANSWER)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getQuestionCategoryDistribution(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuid(quizUuid)
                .stream()
                .map(QuizQuestionFactory::toDTO)
                .collect(Collectors.groupingBy(
                        QuizQuestionDTO::getQuestionCategory, // Using computed property
                        Collectors.counting()
                ));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalQuizPoints(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuid(quizUuid)
                .stream()
                .filter(q -> q.getPoints() != null)
                .map(QuizQuestion::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public double getAverageQuestionPoints(UUID quizUuid) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizUuid(quizUuid);
        if (questions.isEmpty()) {
            return 0.0;
        }

        BigDecimal total = questions.stream()
                .filter(q -> q.getPoints() != null)
                .map(QuizQuestion::getPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(questions.size()), 2, RoundingMode.HALF_UP).doubleValue();
    }

    public void reorderQuestions(UUID quizUuid, List<UUID> questionUuids) {
        for (int i = 0; i < questionUuids.size(); i++) {
            // Each UUID in the body must name a question of this quiz, or reordering one quiz would
            // let a caller renumber another's.
            QuizQuestion question = requireQuestionInQuiz(quizUuid, questionUuids.get(i));

            question.setDisplayOrder(i + 1);
            quizQuestionRepository.save(question);
        }
    }

    @Transactional(readOnly = true)
    public int getNextDisplayOrder(UUID quizUuid) {
        int maxOrder = quizQuestionRepository.findMaxDisplayOrderByQuizUuid(quizUuid);
        return maxOrder + 1;
    }

    @Transactional(readOnly = true)
    public boolean hasQuestions(UUID quizUuid) {
        return quizQuestionRepository.countByQuizUuid(quizUuid) > 0;
    }

    @Transactional(readOnly = true)
    public boolean hasOptionsBasedQuestions(UUID quizUuid) {
        return quizQuestionRepository.existsByQuizUuidAndQuestionTypeIn(
                quizUuid, List.of(QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE));
    }

    @Transactional(readOnly = true)
    public boolean canDeleteQuestion(UUID questionUuid) {
        // Check if any responses exist for this question
        return quizResponseRepository.countByQuestionUuid(questionUuid) == 0;
    }

    @Transactional(readOnly = true)
    public List<QuizQuestionDTO> getQuestionsByType(UUID quizUuid, String questionType) {
        Map<String, String> searchParams = Map.of(
                "quizUuid", quizUuid.toString(),
                "questionType", questionType
        );
        Page<QuizQuestionDTO> questions = search(searchParams, Pageable.unpaged());
        return questions.getContent();
    }

    private void updateQuizQuestionFields(QuizQuestion existingQuizQuestion, QuizQuestionDTO dto) {
        if (dto.quizUuid() != null) {
            existingQuizQuestion.setQuizUuid(dto.quizUuid());
        }
        if (dto.questionText() != null) {
            existingQuizQuestion.setQuestionText(dto.questionText());
        }
        if (dto.questionType() != null) {
            existingQuizQuestion.setQuestionType(dto.questionType());
        }
        if (dto.points() != null) {
            existingQuizQuestion.setPoints(dto.points());
        }
        if (dto.displayOrder() != null) {
            existingQuizQuestion.setDisplayOrder(dto.displayOrder());
        }
    }
}
