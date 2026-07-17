package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.model.QuizResponse;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.QuizResponseRepository;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizGradingServiceImplTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private QuizQuestionOptionRepository quizQuestionOptionRepository;
    @Mock
    private QuizResponseRepository quizResponseRepository;
    @Mock
    private CourseGradeBookService courseGradeBookService;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QuizGradingServiceImpl service;

    private final UUID attemptUuid = UUID.randomUUID();
    private final UUID quizUuid = UUID.randomUUID();
    private final UUID enrollmentUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(quizAttemptRepository.save(any(QuizAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(quizResponseRepository.save(any(QuizResponse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(courseEnrollmentRepository.findByUuid(enrollmentUuid))
                .thenReturn(Optional.of(enrollment()));
    }

    @Test
    void allObjectiveAnswersCorrectGradesAndPasses() {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        UUID opt1 = UUID.randomUUID();
        UUID opt2 = UUID.randomUUID();

        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt()));
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz(BigDecimal.valueOf(70))));
        when(quizQuestionRepository.findByQuizUuid(quizUuid)).thenReturn(List.of(
                question(q1, QuestionType.MULTIPLE_CHOICE, BigDecimal.ONE),
                question(q2, QuestionType.TRUE_FALSE, BigDecimal.ONE)));
        when(quizResponseRepository.findByAttemptUuid(attemptUuid)).thenReturn(List.of(
                response(q1, opt1), response(q2, opt2)));
        when(quizQuestionOptionRepository.findByUuid(opt1)).thenReturn(Optional.of(option(opt1, q1, true)));
        when(quizQuestionOptionRepository.findByUuid(opt2)).thenReturn(Optional.of(option(opt2, q2, true)));

        QuizAttemptDTO result = service.gradeAttempt(attemptUuid);

        assertThat(result.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(result.score()).isEqualByComparingTo("2");
        assertThat(result.maxScore()).isEqualByComparingTo("2");
        assertThat(result.percentage()).isEqualByComparingTo("100.00");
        assertThat(result.isPassed()).isTrue();
        verify(courseGradeBookService).syncQuizAttemptGrade(
                eq(quizUuid), eq(enrollmentUuid), any(), any(), isNull(), any(), isNull(), eq(AttemptStatus.GRADED));
    }

    @Test
    void partiallyCorrectBelowPassingFails() {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        UUID correct = UUID.randomUUID();
        UUID wrong = UUID.randomUUID();

        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt()));
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz(BigDecimal.valueOf(70))));
        when(quizQuestionRepository.findByQuizUuid(quizUuid)).thenReturn(List.of(
                question(q1, QuestionType.MULTIPLE_CHOICE, BigDecimal.ONE),
                question(q2, QuestionType.MULTIPLE_CHOICE, BigDecimal.ONE)));
        when(quizResponseRepository.findByAttemptUuid(attemptUuid)).thenReturn(List.of(
                response(q1, correct), response(q2, wrong)));
        when(quizQuestionOptionRepository.findByUuid(correct)).thenReturn(Optional.of(option(correct, q1, true)));
        when(quizQuestionOptionRepository.findByUuid(wrong)).thenReturn(Optional.of(option(wrong, q2, false)));

        QuizAttemptDTO result = service.gradeAttempt(attemptUuid);

        assertThat(result.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(result.score()).isEqualByComparingTo("1");
        assertThat(result.percentage()).isEqualByComparingTo("50.00");
        assertThat(result.isPassed()).isFalse();
    }

    @Test
    void attemptWithEssayRemainsPendingManualGrading() {
        UUID mc = UUID.randomUUID();
        UUID essay = UUID.randomUUID();
        UUID opt = UUID.randomUUID();

        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt()));
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz(BigDecimal.valueOf(50))));
        when(quizQuestionRepository.findByQuizUuid(quizUuid)).thenReturn(List.of(
                question(mc, QuestionType.MULTIPLE_CHOICE, BigDecimal.ONE),
                question(essay, QuestionType.ESSAY, BigDecimal.valueOf(2))));
        when(quizResponseRepository.findByAttemptUuid(attemptUuid)).thenReturn(List.of(
                response(mc, opt), textResponse(essay)));
        when(quizQuestionOptionRepository.findByUuid(opt)).thenReturn(Optional.of(option(opt, mc, true)));

        QuizAttemptDTO result = service.gradeAttempt(attemptUuid);

        assertThat(result.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(result.score()).isEqualByComparingTo("1");
        assertThat(result.maxScore()).isEqualByComparingTo("3");
        verify(courseGradeBookService).syncQuizAttemptGrade(
                eq(quizUuid), eq(enrollmentUuid), any(), any(), isNull(), any(), isNull(), eq(AttemptStatus.SUBMITTED));
    }

    @Test
    void unansweredObjectiveQuestionScoresZero() {
        UUID q1 = UUID.randomUUID();

        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt()));
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz(null)));
        when(quizQuestionRepository.findByQuizUuid(quizUuid)).thenReturn(List.of(
                question(q1, QuestionType.MULTIPLE_CHOICE, BigDecimal.ONE)));
        when(quizResponseRepository.findByAttemptUuid(attemptUuid)).thenReturn(List.of());

        QuizAttemptDTO result = service.gradeAttempt(attemptUuid);

        assertThat(result.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(result.score()).isEqualByComparingTo("0");
        assertThat(result.percentage()).isEqualByComparingTo("0");
        assertThat(result.isPassed()).isNull();
    }

    @Test
    void manualGradingOfEssayFinalizesAttempt() {
        UUID mc = UUID.randomUUID();
        UUID essay = UUID.randomUUID();
        UUID opt = UUID.randomUUID();
        UUID instructor = UUID.randomUUID();
        QuizResponse essayResponse = textResponse(essay);

        when(quizAttemptRepository.findByUuid(attemptUuid)).thenReturn(Optional.of(attempt()));
        when(quizQuestionRepository.findByUuid(essay))
                .thenReturn(Optional.of(question(essay, QuestionType.ESSAY, BigDecimal.valueOf(2))));
        when(quizResponseRepository.findByAttemptUuidAndQuestionUuid(attemptUuid, essay))
                .thenReturn(Optional.of(essayResponse));
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz(BigDecimal.valueOf(50))));
        when(quizQuestionRepository.findByQuizUuid(quizUuid)).thenReturn(List.of(
                question(mc, QuestionType.MULTIPLE_CHOICE, BigDecimal.ONE),
                question(essay, QuestionType.ESSAY, BigDecimal.valueOf(2))));
        when(quizResponseRepository.findByAttemptUuid(attemptUuid)).thenReturn(List.of(
                response(mc, opt), essayResponse));
        when(quizQuestionOptionRepository.findByUuid(opt)).thenReturn(Optional.of(option(opt, mc, true)));

        QuizAttemptDTO result = service.gradeTextResponse(attemptUuid, essay, BigDecimal.valueOf(2), true, "Great answer", instructor);

        assertThat(result.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(result.score()).isEqualByComparingTo("3");
        assertThat(result.maxScore()).isEqualByComparingTo("3");
        assertThat(result.gradedByUuid()).isEqualTo(instructor);
        assertThat(essayResponse.getPointsEarned()).isEqualByComparingTo("2");
        assertThat(essayResponse.getFeedback()).isEqualTo("Great answer");
        assertThat(essayResponse.getGradedByUuid()).isEqualTo(instructor);
    }

    private QuizAttempt attempt() {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUuid(attemptUuid);
        attempt.setQuizUuid(quizUuid);
        attempt.setEnrollmentUuid(enrollmentUuid);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        return attempt;
    }

    private Quiz quiz(BigDecimal passingScore) {
        Quiz quiz = new Quiz();
        quiz.setUuid(quizUuid);
        quiz.setTitle("Sample quiz");
        quiz.setPassingScore(passingScore);
        return quiz;
    }

    private QuizQuestion question(UUID uuid, QuestionType type, BigDecimal points) {
        QuizQuestion question = new QuizQuestion();
        question.setUuid(uuid);
        question.setQuizUuid(quizUuid);
        question.setQuestionType(type);
        question.setPoints(points);
        return question;
    }

    private QuizQuestionOption option(UUID uuid, UUID questionUuid, boolean correct) {
        QuizQuestionOption option = new QuizQuestionOption();
        option.setUuid(uuid);
        option.setQuestionUuid(questionUuid);
        option.setIsCorrect(correct);
        return option;
    }

    private QuizResponse response(UUID questionUuid, UUID selectedOptionUuid) {
        QuizResponse response = new QuizResponse();
        response.setUuid(UUID.randomUUID());
        response.setAttemptUuid(attemptUuid);
        response.setQuestionUuid(questionUuid);
        response.setSelectedOptionUuid(selectedOptionUuid);
        return response;
    }

    private QuizResponse textResponse(UUID questionUuid) {
        QuizResponse response = new QuizResponse();
        response.setUuid(UUID.randomUUID());
        response.setAttemptUuid(attemptUuid);
        response.setQuestionUuid(questionUuid);
        response.setTextResponse("An essay answer");
        return response;
    }

    private CourseEnrollment enrollment() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUuid(enrollmentUuid);
        enrollment.setStudentUuid(UUID.randomUUID());
        enrollment.setCourseUuid(UUID.randomUUID());
        return enrollment;
    }
}
