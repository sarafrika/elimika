package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import apps.sarafrika.elimika.course.dto.QuizAttemptSubmissionRequest;
import apps.sarafrika.elimika.course.dto.QuizResponseSubmissionDTO;
import apps.sarafrika.elimika.course.factory.QuizAttemptFactory;
import apps.sarafrika.elimika.course.internal.StudentQuizAccessValidator;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.model.QuizResponse;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.QuizResponseRepository;
import apps.sarafrika.elimika.course.service.QuizGradingService;
import apps.sarafrika.elimika.course.service.StudentQuizSubmissionService;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentQuizSubmissionServiceImpl implements StudentQuizSubmissionService {

    private static final String QUIZ_NOT_FOUND_TEMPLATE = "Quiz with ID %s not found";
    private static final String ATTEMPT_NOT_FOUND_TEMPLATE = "Quiz attempt with ID %s not found";
    private static final String QUESTION_NOT_FOUND_TEMPLATE = "Quiz question with ID %s not found";
    private static final String OPTION_NOT_FOUND_TEMPLATE = "Quiz question option with ID %s not found";

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionOptionRepository quizQuestionOptionRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final StudentQuizAccessValidator accessValidator;
    private final QuizGradingService quizGradingService;

    @Override
    public QuizAttemptDTO startAttempt(UUID quizUuid, UUID enrollmentUuid) {
        Quiz quiz = loadQuiz(quizUuid);
        accessValidator.requireEnrollmentAccess(quiz, enrollmentUuid);
        accessValidator.requireStudentVisibleQuiz(quiz);

        QuizAttempt latest = quizAttemptRepository
                .findTopByEnrollmentUuidAndQuizUuidOrderByAttemptNumberDesc(enrollmentUuid, quizUuid)
                .orElse(null);
        if (latest != null && latest.getStatus() == AttemptStatus.IN_PROGRESS) {
            return QuizAttemptFactory.toDTO(latest);
        }

        long attemptCount = quizAttemptRepository.countByEnrollmentUuidAndQuizUuid(enrollmentUuid, quizUuid);
        if (quiz.getAttemptsAllowed() != null && attemptCount >= quiz.getAttemptsAllowed()) {
            throw new IllegalStateException("No remaining attempts are allowed for this quiz.");
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setEnrollmentUuid(enrollmentUuid);
        attempt.setQuizUuid(quizUuid);
        attempt.setAttemptNumber(latest != null ? latest.getAttemptNumber() + 1 : 1);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setMaxScore(totalQuizPoints(quizUuid));

        return QuizAttemptFactory.toDTO(quizAttemptRepository.save(attempt));
    }

    @Override
    public QuizAttemptDTO saveResponses(UUID quizUuid, UUID attemptUuid, UUID enrollmentUuid,
                                        List<QuizResponseSubmissionDTO> responses) {
        Quiz quiz = loadQuiz(quizUuid);
        accessValidator.requireEnrollmentAccess(quiz, enrollmentUuid);
        QuizAttempt attempt = loadOwnedAttempt(attemptUuid, quizUuid, enrollmentUuid);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("Answers can only be saved while the attempt is in progress.");
        }
        if (responses != null) {
            responses.forEach(response -> upsertResponse(quizUuid, attempt.getUuid(), response));
        }
        return QuizAttemptFactory.toDTO(attempt);
    }

    @Override
    public QuizAttemptDTO submitAttempt(UUID quizUuid, UUID attemptUuid, UUID enrollmentUuid,
                                        QuizAttemptSubmissionRequest request) {
        if (request != null && request.responses() != null && !request.responses().isEmpty()) {
            saveResponses(quizUuid, attemptUuid, enrollmentUuid, request.responses());
        }

        Quiz quiz = loadQuiz(quizUuid);
        accessValidator.requireEnrollmentAccess(quiz, enrollmentUuid);
        QuizAttempt attempt = loadOwnedAttempt(attemptUuid, quizUuid, enrollmentUuid);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("This attempt has already been submitted.");
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        attempt.setSubmittedAt(submittedAt);
        attempt.setTimeTakenMinutes(resolveTimeTaken(request, attempt.getStartedAt(), submittedAt));
        attempt.setStatus(AttemptStatus.SUBMITTED);
        quizAttemptRepository.save(attempt);

        return quizGradingService.gradeAttempt(attemptUuid);
    }

    private void upsertResponse(UUID quizUuid, UUID attemptUuid, QuizResponseSubmissionDTO submission) {
        QuizQuestion question = quizQuestionRepository.findByUuid(submission.questionUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUESTION_NOT_FOUND_TEMPLATE, submission.questionUuid())));
        if (!quizUuid.equals(question.getQuizUuid())) {
            throw new IllegalArgumentException("Question does not belong to this quiz.");
        }
        if (submission.selectedOptionUuid() != null) {
            QuizQuestionOption option = quizQuestionOptionRepository.findByUuid(submission.selectedOptionUuid())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(OPTION_NOT_FOUND_TEMPLATE, submission.selectedOptionUuid())));
            if (!question.getUuid().equals(option.getQuestionUuid())) {
                throw new IllegalArgumentException("Selected option does not belong to the question.");
            }
        }

        QuizResponse response = quizResponseRepository
                .findByAttemptUuidAndQuestionUuid(attemptUuid, submission.questionUuid())
                .orElseGet(QuizResponse::new);
        response.setAttemptUuid(attemptUuid);
        response.setQuestionUuid(submission.questionUuid());
        response.setSelectedOptionUuid(submission.selectedOptionUuid());
        response.setTextResponse(submission.textResponse());
        // Not graded yet: clear any prior scoring so a re-saved answer is regraded on submit.
        response.setPointsEarned(null);
        response.setIsCorrect(null);
        quizResponseRepository.save(response);
    }

    private QuizAttempt loadOwnedAttempt(UUID attemptUuid, UUID quizUuid, UUID enrollmentUuid) {
        QuizAttempt attempt = quizAttemptRepository.findByUuid(attemptUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ATTEMPT_NOT_FOUND_TEMPLATE, attemptUuid)));
        if (!quizUuid.equals(attempt.getQuizUuid()) || !enrollmentUuid.equals(attempt.getEnrollmentUuid())) {
            throw new AccessDeniedException("Quiz attempt does not belong to the requested enrollment.");
        }
        return attempt;
    }

    private Integer resolveTimeTaken(QuizAttemptSubmissionRequest request, LocalDateTime startedAt, LocalDateTime submittedAt) {
        if (request != null && request.timeTakenMinutes() != null) {
            return request.timeTakenMinutes();
        }
        if (startedAt == null) {
            return null;
        }
        return (int) Math.max(0, Duration.between(startedAt, submittedAt).toMinutes());
    }

    private BigDecimal totalQuizPoints(UUID quizUuid) {
        return quizQuestionRepository.findByQuizUuid(quizUuid).stream()
                .map(QuizQuestion::getPoints)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Quiz loadQuiz(UUID quizUuid) {
        return quizRepository.findByUuid(quizUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_NOT_FOUND_TEMPLATE, quizUuid)));
    }
}
