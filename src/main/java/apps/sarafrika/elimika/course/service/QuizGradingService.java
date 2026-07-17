package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.QuizAttemptDTO;

import java.math.BigDecimal;
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

    /**
     * Records an instructor's grade for a single short-answer/essay response on a submitted
     * attempt, then recomputes attempt totals. Once every answered manual question is graded the
     * attempt finalises to {@code GRADED} and the grade re-syncs to the gradebook.
     *
     * @param attemptUuid   the submitted attempt
     * @param questionUuid  the manually graded question
     * @param points        points to award (clamped to the question's maximum)
     * @param correct       optional correctness flag for the response
     * @param feedback      optional instructor feedback
     * @param gradedByUuid  the grading instructor
     * @return the updated attempt
     */
    QuizAttemptDTO gradeTextResponse(UUID attemptUuid, UUID questionUuid, BigDecimal points,
                                     Boolean correct, String feedback, UUID gradedByUuid);
}
