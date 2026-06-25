package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.StudentQuizDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizQuestionDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizQuestionOptionDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizReviewDTO;
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
import apps.sarafrika.elimika.course.service.StudentQuizViewService;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentQuizViewServiceImpl implements StudentQuizViewService {

    private static final String QUIZ_NOT_FOUND_TEMPLATE = "Quiz with ID %s not found";
    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Course enrollment with ID %s not found";
    private static final String ATTEMPT_NOT_FOUND_TEMPLATE = "Quiz attempt with ID %s not found";

    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizQuestionOptionRepository quizQuestionOptionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizResponseRepository quizResponseRepository;
    private final DomainSecurityService domainSecurityService;

    @Override
    public StudentQuizDTO getStudentQuiz(UUID quizUuid, UUID enrollmentUuid) {
        Quiz quiz = loadQuiz(quizUuid);
        CourseEnrollment enrollment = loadEnrollment(enrollmentUuid);
        UUID courseUuid = resolveCourseUuid(quiz);

        validateEnrollmentAccess(enrollment, courseUuid);
        validateStudentVisibleQuiz(quiz);

        List<StudentQuizQuestionDTO> questions = quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(quizUuid)
                .stream()
                .map(this::toStudentQuestion)
                .toList();

        return new StudentQuizDTO(
                quiz.getUuid(),
                quiz.getLessonUuid(),
                quiz.getScope(),
                quiz.getClassDefinitionUuid(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getInstructions(),
                quiz.getTimeLimitMinutes(),
                quiz.getAttemptsAllowed(),
                quiz.getPassingScore(),
                questions
        );
    }

    @Override
    public StudentQuizReviewDTO getStudentQuizReview(UUID quizUuid, UUID attemptUuid, UUID enrollmentUuid) {
        Quiz quiz = loadQuiz(quizUuid);
        CourseEnrollment enrollment = loadEnrollment(enrollmentUuid);
        UUID courseUuid = resolveCourseUuid(quiz);
        validateEnrollmentAccess(enrollment, courseUuid);

        QuizAttempt attempt = quizAttemptRepository.findByUuid(attemptUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ATTEMPT_NOT_FOUND_TEMPLATE, attemptUuid)));

        if (!quizUuid.equals(attempt.getQuizUuid()) || !enrollmentUuid.equals(attempt.getEnrollmentUuid())) {
            throw new AccessDeniedException("Quiz attempt does not belong to the requested enrollment.");
        }
        if (attempt.getStatus() != AttemptStatus.GRADED) {
            throw new IllegalStateException("Quiz review is available only after the attempt has been graded.");
        }

        Map<UUID, QuizResponse> responsesByQuestion = quizResponseRepository.findByAttemptUuid(attemptUuid)
                .stream()
                .collect(Collectors.toMap(
                        QuizResponse::getQuestionUuid,
                        Function.identity(),
                        this::latestResponse
                ));

        List<StudentQuizReviewDTO.QuestionReviewDTO> questionReviews =
                quizQuestionRepository.findByQuizUuidOrderByDisplayOrderAsc(quizUuid)
                        .stream()
                        .map(question -> toReviewQuestion(question, responsesByQuestion.get(question.getUuid())))
                        .toList();

        return new StudentQuizReviewDTO(
                attempt.getQuizUuid(),
                attempt.getUuid(),
                attempt.getEnrollmentUuid(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getPercentage(),
                attempt.getIsPassed(),
                questionReviews
        );
    }

    private StudentQuizQuestionDTO toStudentQuestion(QuizQuestion question) {
        List<StudentQuizQuestionOptionDTO> options =
                quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(question.getUuid())
                        .stream()
                        .map(this::toStudentOption)
                        .toList();

        return new StudentQuizQuestionDTO(
                question.getUuid(),
                question.getQuizUuid(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getPoints(),
                question.getDisplayOrder(),
                options
        );
    }

    private StudentQuizQuestionOptionDTO toStudentOption(QuizQuestionOption option) {
        return new StudentQuizQuestionOptionDTO(
                option.getUuid(),
                option.getQuestionUuid(),
                option.getOptionText(),
                option.getDisplayOrder()
        );
    }

    private StudentQuizReviewDTO.QuestionReviewDTO toReviewQuestion(QuizQuestion question, QuizResponse response) {
        List<StudentQuizReviewDTO.OptionReviewDTO> options =
                quizQuestionOptionRepository.findByQuestionUuidOrderByDisplayOrderAsc(question.getUuid())
                        .stream()
                        .map(this::toReviewOption)
                        .toList();

        return new StudentQuizReviewDTO.QuestionReviewDTO(
                question.getUuid(),
                question.getQuizUuid(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getPoints(),
                question.getDisplayOrder(),
                toReviewResponse(response),
                options
        );
    }

    private StudentQuizReviewDTO.OptionReviewDTO toReviewOption(QuizQuestionOption option) {
        return new StudentQuizReviewDTO.OptionReviewDTO(
                option.getUuid(),
                option.getQuestionUuid(),
                option.getOptionText(),
                option.getIsCorrect(),
                option.getDisplayOrder()
        );
    }

    private StudentQuizReviewDTO.ResponseReviewDTO toReviewResponse(QuizResponse response) {
        if (response == null) {
            return null;
        }

        return new StudentQuizReviewDTO.ResponseReviewDTO(
                response.getUuid(),
                response.getAttemptUuid(),
                response.getQuestionUuid(),
                response.getSelectedOptionUuid(),
                response.getTextResponse(),
                response.getPointsEarned(),
                response.getIsCorrect()
        );
    }

    private Quiz loadQuiz(UUID quizUuid) {
        return quizRepository.findByUuid(quizUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_NOT_FOUND_TEMPLATE, quizUuid)));
    }

    private CourseEnrollment loadEnrollment(UUID enrollmentUuid) {
        return courseEnrollmentRepository.findByUuid(enrollmentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, enrollmentUuid)));
    }

    private UUID resolveCourseUuid(Quiz quiz) {
        Lesson lesson = lessonRepository.findByUuid(quiz.getLessonUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Lesson with ID %s not found", quiz.getLessonUuid())));
        return lesson.getCourseUuid();
    }

    private void validateEnrollmentAccess(CourseEnrollment enrollment, UUID courseUuid) {
        if (!courseUuid.equals(enrollment.getCourseUuid())) {
            throw new AccessDeniedException("Course enrollment does not belong to this quiz.");
        }
        if (enrollment.getStatus() == null || !enrollment.getStatus().allowsAccess()) {
            throw new AccessDeniedException("Course enrollment does not allow quiz access.");
        }
        if (domainSecurityService.isStudent()
                && !domainSecurityService.isStudentWithUuid(enrollment.getStudentUuid())) {
            throw new AccessDeniedException("Students may only access quizzes for their own course enrollment.");
        }
    }

    private void validateStudentVisibleQuiz(Quiz quiz) {
        if (quiz.getStatus() != ContentStatus.PUBLISHED || !Boolean.TRUE.equals(quiz.getActive())) {
            throw new ResourceNotFoundException(String.format(QUIZ_NOT_FOUND_TEMPLATE, quiz.getUuid()));
        }
    }

    private QuizResponse latestResponse(QuizResponse first, QuizResponse second) {
        LocalDateTime firstTimestamp = responseTimestamp(first);
        LocalDateTime secondTimestamp = responseTimestamp(second);
        if (firstTimestamp == null) {
            return second;
        }
        if (secondTimestamp == null) {
            return first;
        }
        return firstTimestamp.isAfter(secondTimestamp) ? first : second;
    }

    private LocalDateTime responseTimestamp(QuizResponse response) {
        if (response.getLastModifiedDate() != null) {
            return response.getLastModifiedDate();
        }
        return response.getCreatedDate();
    }
}
