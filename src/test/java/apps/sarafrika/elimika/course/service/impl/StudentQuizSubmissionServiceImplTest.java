package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import apps.sarafrika.elimika.course.dto.QuizAttemptSubmissionRequest;
import apps.sarafrika.elimika.course.dto.QuizResponseSubmissionDTO;
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
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentQuizSubmissionServiceImplTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private QuizQuestionOptionRepository quizQuestionOptionRepository;
    @Mock
    private QuizResponseRepository quizResponseRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private StudentQuizAccessValidator accessValidator;
    @Mock
    private QuizGradingService quizGradingService;

    @InjectMocks
    private StudentQuizSubmissionServiceImpl service;

    private final UUID quizUuid = UUID.randomUUID();
    private final UUID enrollmentUuid = UUID.randomUUID();
    private final UUID attemptUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz(2)));
        lenient().when(quizAttemptRepository.save(any(QuizAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(quizQuestionRepository.findByQuizUuid(quizUuid)).thenReturn(List.of());
    }

    @Test
    void startCreatesFirstAttempt() {
        when(quizAttemptRepository.findTopByEnrollmentUuidAndQuizUuidOrderByAttemptNumberDesc(enrollmentUuid, quizUuid))
                .thenReturn(Optional.empty());
        when(quizAttemptRepository.countByEnrollmentUuidAndQuizUuid(enrollmentUuid, quizUuid)).thenReturn(0L);

        QuizAttemptDTO result = service.startAttempt(quizUuid, enrollmentUuid);

        assertThat(result.attemptNumber()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    void startResumesInProgressAttempt() {
        QuizAttempt inProgress = attempt(AttemptStatus.IN_PROGRESS);
        inProgress.setAttemptNumber(1);
        when(quizAttemptRepository.findTopByEnrollmentUuidAndQuizUuidOrderByAttemptNumberDesc(enrollmentUuid, quizUuid))
                .thenReturn(Optional.of(inProgress));

        QuizAttemptDTO result = service.startAttempt(quizUuid, enrollmentUuid);

        assertThat(result.attemptNumber()).isEqualTo(1);
        verify(quizAttemptRepository, never()).save(any());
    }

    @Test
    void startRejectsWhenNoAttemptsRemain() {
        QuizAttempt graded = attempt(AttemptStatus.GRADED);
        graded.setAttemptNumber(2);
        when(quizAttemptRepository.findTopByEnrollmentUuidAndQuizUuidOrderByAttemptNumberDesc(enrollmentUuid, quizUuid))
                .thenReturn(Optional.of(graded));
        when(quizAttemptRepository.countByEnrollmentUuidAndQuizUuid(enrollmentUuid, quizUuid)).thenReturn(2L);

        assertThatThrownBy(() -> service.startAttempt(quizUuid, enrollmentUuid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No remaining attempts");
    }

    @Test
    void saveResponsesRejectsWhenNotInProgress() {
        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt(AttemptStatus.SUBMITTED)));

        assertThatThrownBy(() -> service.saveResponses(quizUuid, attemptUuid, enrollmentUuid,
                List.of(new QuizResponseSubmissionDTO(UUID.randomUUID(), UUID.randomUUID(), null))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("in progress");
    }

    @Test
    void saveResponsesUpsertsSelectedOption() {
        UUID questionUuid = UUID.randomUUID();
        UUID optionUuid = UUID.randomUUID();
        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt(AttemptStatus.IN_PROGRESS)));
        when(quizQuestionRepository.findByUuid(questionUuid)).thenReturn(Optional.of(question(questionUuid)));
        when(quizQuestionOptionRepository.findByUuid(optionUuid)).thenReturn(Optional.of(option(optionUuid, questionUuid)));
        when(quizResponseRepository.findByAttemptUuidAndQuestionUuid(attemptUuid, questionUuid))
                .thenReturn(Optional.empty());

        service.saveResponses(quizUuid, attemptUuid, enrollmentUuid,
                List.of(new QuizResponseSubmissionDTO(questionUuid, optionUuid, null)));

        ArgumentCaptor<QuizResponse> captor = ArgumentCaptor.forClass(QuizResponse.class);
        verify(quizResponseRepository).save(captor.capture());
        assertThat(captor.getValue().getSelectedOptionUuid()).isEqualTo(optionUuid);
        assertThat(captor.getValue().getQuestionUuid()).isEqualTo(questionUuid);
    }

    @Test
    void saveResponsesRejectsQuestionFromAnotherQuiz() {
        UUID questionUuid = UUID.randomUUID();
        QuizQuestion foreign = question(questionUuid);
        foreign.setQuizUuid(UUID.randomUUID());
        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt(AttemptStatus.IN_PROGRESS)));
        when(quizQuestionRepository.findByUuid(questionUuid)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.saveResponses(quizUuid, attemptUuid, enrollmentUuid,
                List.of(new QuizResponseSubmissionDTO(questionUuid, null, "text"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to this quiz");
    }

    @Test
    void saveResponsesRejectsForeignAttempt() {
        QuizAttempt foreign = attempt(AttemptStatus.IN_PROGRESS);
        foreign.setEnrollmentUuid(UUID.randomUUID());
        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.saveResponses(quizUuid, attemptUuid, enrollmentUuid, List.of()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong to the requested enrollment");
    }

    @Test
    void submitTransitionsToSubmittedAndGrades() {
        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt(AttemptStatus.IN_PROGRESS)));
        QuizAttemptDTO gradedDto = gradedDto();
        when(quizGradingService.gradeAttempt(attemptUuid)).thenReturn(gradedDto);

        QuizAttemptDTO result = service.submitAttempt(quizUuid, attemptUuid, enrollmentUuid,
                new QuizAttemptSubmissionRequest(null, 5));

        assertThat(result).isEqualTo(gradedDto);
        ArgumentCaptor<QuizAttempt> captor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(captor.getValue().getTimeTakenMinutes()).isEqualTo(5);
        verify(quizGradingService).gradeAttempt(attemptUuid);
    }

    @Test
    void submitRejectsAlreadySubmittedAttempt() {
        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt(AttemptStatus.GRADED)));

        assertThatThrownBy(() -> service.submitAttempt(quizUuid, attemptUuid, enrollmentUuid, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been submitted");
        verify(quizGradingService, never()).gradeAttempt(any());
    }

    private Quiz quiz(Integer attemptsAllowed) {
        Quiz quiz = new Quiz();
        quiz.setUuid(quizUuid);
        quiz.setAttemptsAllowed(attemptsAllowed);
        return quiz;
    }

    private QuizAttempt attempt(AttemptStatus status) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUuid(attemptUuid);
        attempt.setQuizUuid(quizUuid);
        attempt.setEnrollmentUuid(enrollmentUuid);
        attempt.setStatus(status);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        return attempt;
    }

    private QuizQuestion question(UUID uuid) {
        QuizQuestion question = new QuizQuestion();
        question.setUuid(uuid);
        question.setQuizUuid(quizUuid);
        return question;
    }

    private QuizQuestionOption option(UUID uuid, UUID questionUuid) {
        QuizQuestionOption option = new QuizQuestionOption();
        option.setUuid(uuid);
        option.setQuestionUuid(questionUuid);
        return option;
    }

    private QuizAttemptDTO gradedDto() {
        return new QuizAttemptDTO(attemptUuid, enrollmentUuid, quizUuid, 1, LocalDateTime.now(), LocalDateTime.now(),
                5, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.valueOf(100), true, AttemptStatus.GRADED,
                null, LocalDateTime.now(), LocalDateTime.now(), "student", LocalDateTime.now(), "student");
    }
}
