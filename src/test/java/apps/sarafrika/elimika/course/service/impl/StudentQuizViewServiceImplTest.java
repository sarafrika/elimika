package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.StudentQuizDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizReviewDTO;
import apps.sarafrika.elimika.course.internal.StudentQuizAccessValidator;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.model.QuizAttempt;
import apps.sarafrika.elimika.course.model.QuizQuestion;
import apps.sarafrika.elimika.course.model.QuizQuestionOption;
import apps.sarafrika.elimika.course.model.QuizResponse;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionOptionRepository;
import apps.sarafrika.elimika.course.repository.QuizQuestionRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.QuizResponseRepository;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import apps.sarafrika.elimika.course.util.enums.QuizScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentQuizViewServiceImplTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private QuizQuestionOptionRepository quizQuestionOptionRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizResponseRepository quizResponseRepository;
    @Mock
    private DomainSecurityService domainSecurityService;

    private StudentQuizViewServiceImpl service;

    @BeforeEach
    void setUp() {
        StudentQuizAccessValidator accessValidator = new StudentQuizAccessValidator(
                lessonRepository,
                courseEnrollmentRepository,
                domainSecurityService
        );
        service = new StudentQuizViewServiceImpl(
                quizRepository,
                quizQuestionRepository,
                quizQuestionOptionRepository,
                quizAttemptRepository,
                quizResponseRepository,
                accessValidator
        );
    }

    @Test
    void studentQuizViewReturnsOptionsWithoutCorrectnessForOwnEnrollment() {
        UUID quizUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID questionUuid = UUID.randomUUID();
        UUID optionUuid = UUID.randomUUID();

        stubQuizCourseAndEnrollment(quizUuid, lessonUuid, courseUuid, enrollmentUuid, studentUuid);
        when(domainSecurityService.isStudent()).thenReturn(true);
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(quizUuid))
                .thenReturn(List.of(question(questionUuid, quizUuid)));
        when(quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(questionUuid))
                .thenReturn(List.of(option(optionUuid, questionUuid, true)));

        StudentQuizDTO result = service.getStudentQuiz(quizUuid, enrollmentUuid);

        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().getFirst().options()).hasSize(1);
        assertThat(result.questions().getFirst().options().getFirst().uuid()).isEqualTo(optionUuid);
        assertThat(result.questions().getFirst().options().getFirst().optionText()).isEqualTo("Option 1");
    }

    @Test
    void studentQuizViewRejectsAnotherStudentsEnrollment() {
        UUID quizUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();

        stubQuizCourseAndEnrollment(quizUuid, lessonUuid, courseUuid, enrollmentUuid, studentUuid);
        when(domainSecurityService.isStudent()).thenReturn(true);
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(false);

        assertThatThrownBy(() -> service.getStudentQuiz(quizUuid, enrollmentUuid))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("own course enrollment");

        verify(quizQuestionRepository, never()).findByQuizUuidOrderByDisplayOrderAsc(quizUuid);
    }

    @Test
    void gradedReviewIncludesStudentResponseAndCorrectAnswerChoices() {
        UUID quizUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID attemptUuid = UUID.randomUUID();
        UUID questionUuid = UUID.randomUUID();
        UUID correctOptionUuid = UUID.randomUUID();
        UUID responseUuid = UUID.randomUUID();

        stubQuizCourseAndEnrollment(quizUuid, lessonUuid, courseUuid, enrollmentUuid, studentUuid);
        when(domainSecurityService.isStudent()).thenReturn(true);
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(quizAttemptRepository.findByUuid(attemptUuid))
                .thenReturn(Optional.of(attempt(attemptUuid, quizUuid, enrollmentUuid, AttemptStatus.GRADED)));
        when(quizResponseRepository.findByAttemptUuid(attemptUuid))
                .thenReturn(List.of(response(responseUuid, attemptUuid, questionUuid, correctOptionUuid, true)));
        when(quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(quizUuid))
                .thenReturn(List.of(question(questionUuid, quizUuid)));
        when(quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(questionUuid))
                .thenReturn(List.of(option(correctOptionUuid, questionUuid, true)));

        StudentQuizReviewDTO review = service.getStudentQuizReview(quizUuid, attemptUuid, enrollmentUuid);

        assertThat(review.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(review.questions()).hasSize(1);
        assertThat(review.questions().getFirst().response().isCorrect()).isTrue();
        assertThat(review.questions().getFirst().response().selectedOptionUuid()).isEqualTo(correctOptionUuid);
        assertThat(review.questions().getFirst().options().getFirst().isCorrect()).isTrue();
    }

    @Test
    void reviewRejectsAttemptBeforeGrading() {
        UUID quizUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID attemptUuid = UUID.randomUUID();

        stubQuizCourseAndEnrollment(quizUuid, lessonUuid, courseUuid, enrollmentUuid, studentUuid);
        when(domainSecurityService.isStudent()).thenReturn(true);
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(quizAttemptRepository.findByUuid(attemptUuid))
                .thenReturn(Optional.of(attempt(attemptUuid, quizUuid, enrollmentUuid, AttemptStatus.SUBMITTED)));

        assertThatThrownBy(() -> service.getStudentQuizReview(quizUuid, attemptUuid, enrollmentUuid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only after the attempt has been graded");

        verify(quizResponseRepository, never()).findByAttemptUuid(attemptUuid);
    }

    @Test
    void instructorMayReviewSubmittedAttemptForGrading() {
        UUID quizUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        UUID attemptUuid = UUID.randomUUID();
        UUID questionUuid = UUID.randomUUID();
        UUID correctOptionUuid = UUID.randomUUID();
        UUID responseUuid = UUID.randomUUID();

        stubQuizCourseAndEnrollment(quizUuid, lessonUuid, courseUuid, enrollmentUuid, studentUuid);
        when(domainSecurityService.isInstructorOrAdmin()).thenReturn(true);
        when(quizAttemptRepository.findByUuid(attemptUuid))
                .thenReturn(Optional.of(attempt(attemptUuid, quizUuid, enrollmentUuid, AttemptStatus.SUBMITTED)));
        when(quizResponseRepository.findByAttemptUuid(attemptUuid))
                .thenReturn(List.of(response(responseUuid, attemptUuid, questionUuid, correctOptionUuid, true)));
        when(quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(quizUuid))
                .thenReturn(List.of(question(questionUuid, quizUuid)));
        when(quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(questionUuid))
                .thenReturn(List.of(option(correctOptionUuid, questionUuid, true)));

        StudentQuizReviewDTO review = service.getStudentQuizReview(quizUuid, attemptUuid, enrollmentUuid);

        assertThat(review.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(review.questions()).hasSize(1);
    }

    private void stubQuizCourseAndEnrollment(UUID quizUuid,
                                             UUID lessonUuid,
                                             UUID courseUuid,
                                             UUID enrollmentUuid,
                                             UUID studentUuid) {
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz(quizUuid, lessonUuid)));
        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(lessonUuid, courseUuid)));
        when(courseEnrollmentRepository.findByUuid(enrollmentUuid))
                .thenReturn(Optional.of(enrollment(enrollmentUuid, studentUuid, courseUuid)));
    }

    private Quiz quiz(UUID quizUuid, UUID lessonUuid) {
        Quiz quiz = new Quiz();
        quiz.setUuid(quizUuid);
        quiz.setLessonUuid(lessonUuid);
        quiz.setScope(QuizScope.COURSE_TEMPLATE);
        quiz.setTitle("Safety quiz");
        quiz.setDescription("Security quiz");
        quiz.setInstructions("Choose one");
        quiz.setTimeLimitMinutes(15);
        quiz.setAttemptsAllowed(1);
        quiz.setPassingScore(BigDecimal.valueOf(70));
        quiz.setStatus(ContentStatus.PUBLISHED);
        quiz.setActive(true);
        return quiz;
    }

    private Lesson lesson(UUID lessonUuid, UUID courseUuid) {
        Lesson lesson = new Lesson();
        lesson.setUuid(lessonUuid);
        lesson.setCourseUuid(courseUuid);
        return lesson;
    }

    private CourseEnrollment enrollment(UUID enrollmentUuid, UUID studentUuid, UUID courseUuid) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUuid(enrollmentUuid);
        enrollment.setStudentUuid(studentUuid);
        enrollment.setCourseUuid(courseUuid);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }

    private QuizQuestion question(UUID questionUuid, UUID quizUuid) {
        QuizQuestion question = new QuizQuestion();
        question.setUuid(questionUuid);
        question.setQuizUuid(quizUuid);
        question.setQuestionText("Which option is safe?");
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        question.setPoints(BigDecimal.ONE);
        question.setDisplayOrder(1);
        return question;
    }

    private QuizQuestionOption option(UUID optionUuid, UUID questionUuid, boolean correct) {
        QuizQuestionOption option = new QuizQuestionOption();
        option.setUuid(optionUuid);
        option.setQuestionUuid(questionUuid);
        option.setOptionText("Option 1");
        option.setIsCorrect(correct);
        option.setDisplayOrder(1);
        return option;
    }

    private QuizAttempt attempt(UUID attemptUuid, UUID quizUuid, UUID enrollmentUuid, AttemptStatus status) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUuid(attemptUuid);
        attempt.setQuizUuid(quizUuid);
        attempt.setEnrollmentUuid(enrollmentUuid);
        attempt.setStatus(status);
        attempt.setScore(BigDecimal.ONE);
        attempt.setMaxScore(BigDecimal.ONE);
        attempt.setPercentage(BigDecimal.valueOf(100));
        attempt.setIsPassed(true);
        return attempt;
    }

    private QuizResponse response(UUID responseUuid,
                                  UUID attemptUuid,
                                  UUID questionUuid,
                                  UUID selectedOptionUuid,
                                  boolean correct) {
        QuizResponse response = new QuizResponse();
        response.setUuid(responseUuid);
        response.setAttemptUuid(attemptUuid);
        response.setQuestionUuid(questionUuid);
        response.setSelectedOptionUuid(selectedOptionUuid);
        response.setPointsEarned(BigDecimal.ONE);
        response.setIsCorrect(correct);
        return response;
    }
}
