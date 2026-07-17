package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;
import apps.sarafrika.elimika.course.dto.QuizAttemptSubmissionRequest;
import apps.sarafrika.elimika.course.dto.QuizResponseSubmissionDTO;

import java.util.List;
import java.util.UUID;

/**
 * Student-facing quiz-taking flow: starting an attempt, saving answers as the student works,
 * and submitting for grading. All operations enforce that the caller owns the enrollment and
 * that the quiz is visible; scoring is never accepted from the client.
 */
public interface StudentQuizSubmissionService {

    /**
     * Starts (or resumes) a quiz attempt for the enrollment. Resumes the existing in-progress
     * attempt when one exists; otherwise creates the next attempt, enforcing the quiz's
     * attempts-allowed limit.
     */
    QuizAttemptDTO startAttempt(UUID quizUuid, UUID enrollmentUuid);

    /**
     * Upserts the given answers onto an in-progress attempt (autosave). Answers may be saved
     * repeatedly until the attempt is submitted.
     */
    QuizAttemptDTO saveResponses(UUID quizUuid, UUID attemptUuid, UUID enrollmentUuid,
                                 List<QuizResponseSubmissionDTO> responses);

    /**
     * Saves any answers included in the request, then submits the attempt and grades it.
     */
    QuizAttemptDTO submitAttempt(UUID quizUuid, UUID attemptUuid, UUID enrollmentUuid,
                                 QuizAttemptSubmissionRequest request);
}
