package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.StudentQuizDTO;
import apps.sarafrika.elimika.course.dto.StudentQuizReviewDTO;

import java.util.UUID;

public interface StudentQuizViewService {

    StudentQuizDTO getStudentQuiz(UUID quizUuid, UUID enrollmentUuid);

    StudentQuizReviewDTO getStudentQuizReview(UUID quizUuid, UUID attemptUuid, UUID enrollmentUuid);
}
