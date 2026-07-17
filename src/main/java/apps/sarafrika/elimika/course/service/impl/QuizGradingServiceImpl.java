package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import apps.sarafrika.elimika.course.factory.QuizAttemptFactory;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizResponse;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.QuizResponseRepository;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.service.QuizGradingService;
import apps.sarafrika.elimika.course.spi.AssessmentCompletedNotificationRequestedEvent;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizGradingServiceImpl implements QuizGradingService {

    private static final String ATTEMPT_NOT_FOUND_TEMPLATE = "Quiz attempt with ID %s not found";
    private static final String QUIZ_NOT_FOUND_TEMPLATE = "Quiz with ID %s not found";
    private static final int PERCENTAGE_SCALE = 2;

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionOptionRepository quizQuestionOptionRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final CourseGradeBookService courseGradeBookService;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public QuizAttemptDTO gradeAttempt(UUID attemptUuid) {
        QuizAttempt attempt = quizAttemptRepository.findByUuid(attemptUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ATTEMPT_NOT_FOUND_TEMPLATE, attemptUuid)));
        Quiz quiz = quizRepository.findByUuid(attempt.getQuizUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_NOT_FOUND_TEMPLATE, attempt.getQuizUuid())));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizUuid(quiz.getUuid());
        Map<UUID, QuizResponse> responsesByQuestion = quizResponseRepository.findByAttemptUuid(attemptUuid)
                .stream()
                .collect(Collectors.toMap(QuizResponse::getQuestionUuid, Function.identity(), (a, b) -> b));

        BigDecimal maxScore = BigDecimal.ZERO;
        BigDecimal score = BigDecimal.ZERO;
        boolean pendingManualGrading = false;

        for (QuizQuestion question : questions) {
            BigDecimal questionPoints = nz(question.getPoints());
            maxScore = maxScore.add(questionPoints);
            QuizResponse response = responsesByQuestion.get(question.getUuid());

            if (question.getQuestionType() != null && question.getQuestionType().isAutoGradable()) {
                boolean correct = response != null
                        && response.getSelectedOptionUuid() != null
                        && isOptionCorrect(response.getSelectedOptionUuid(), question.getUuid());
                BigDecimal earned = correct ? questionPoints : BigDecimal.ZERO;
                if (response != null) {
                    response.setPointsEarned(earned);
                    response.setIsCorrect(correct);
                    quizResponseRepository.save(response);
                }
                score = score.add(earned);
            } else {
                pendingManualGrading = true;
                if (response != null && response.getPointsEarned() != null) {
                    score = score.add(response.getPointsEarned());
                }
            }
        }

        BigDecimal percentage = maxScore.compareTo(BigDecimal.ZERO) > 0
                ? score.divide(maxScore, PERCENTAGE_SCALE + 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Boolean passed = quiz.getPassingScore() == null
                ? null
                : percentage.compareTo(quiz.getPassingScore()) >= 0;
        AttemptStatus status = pendingManualGrading ? AttemptStatus.SUBMITTED : AttemptStatus.GRADED;

        attempt.setScore(score);
        attempt.setMaxScore(maxScore);
        attempt.setPercentage(percentage);
        attempt.setIsPassed(passed);
        attempt.setStatus(status);
        QuizAttempt graded = quizAttemptRepository.save(attempt);

        LocalDateTime gradedAt = graded.getSubmittedAt() != null ? graded.getSubmittedAt() : LocalDateTime.now();
        courseGradeBookService.syncQuizAttemptGrade(
                graded.getQuizUuid(),
                graded.getEnrollmentUuid(),
                graded.getScore(),
                graded.getMaxScore(),
                null,
                gradedAt,
                null,
                graded.getStatus()
        );

        publishCompletionNotification(graded, quiz);
        return QuizAttemptFactory.toDTO(graded);
    }

    private boolean isOptionCorrect(UUID optionUuid, UUID questionUuid) {
        return quizQuestionOptionRepository.findByUuid(optionUuid)
                .filter(option -> questionUuid.equals(option.getQuestionUuid()))
                .map(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .orElse(false);
    }

    private void publishCompletionNotification(QuizAttempt attempt, Quiz quiz) {
        if (attempt.getStatus() == null || !attempt.getStatus().isCompleted() || attempt.getEnrollmentUuid() == null) {
            return;
        }
        CourseEnrollment enrollment = courseEnrollmentRepository.findByUuid(attempt.getEnrollmentUuid()).orElse(null);
        if (enrollment == null || enrollment.getStudentUuid() == null) {
            return;
        }

        String title = quiz.getTitle() == null || quiz.getTitle().isBlank() ? "Quiz" : quiz.getTitle();
        eventPublisher.publishEvent(new AssessmentCompletedNotificationRequestedEvent(
                enrollment.getStudentUuid(),
                enrollment.getCourseUuid(),
                attempt.getEnrollmentUuid(),
                attempt.getQuizUuid(),
                attempt.getUuid(),
                title,
                "quiz"
        ));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
