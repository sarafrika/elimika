package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;

import java.util.UUID;

/**
 * Grades quiz attempts. Objective questions (multiple-choice, true/false) are scored
 * automatically from the option answer key; text questions (short-answer, essay) are left
 * pending manual grading. Computes attempt totals, resolves pass/fail, transitions the
 * attempt status, and syncs the derived grade to the gradebook.
 */
public interface QuizGradingService {

    /**
     * Auto-grades every objective response on the attempt and recomputes attempt totals.
     * The attempt becomes {@code GRADED} when no manual grading remains, otherwise
     * {@code SUBMITTED} (awaiting instructor grading of text answers).
     *
     * @param attemptUuid the attempt to grade
     * @return the updated attempt
     */
    QuizAttemptDTO gradeAttempt(UUID attemptUuid);
}
