package apps.sarafrika.elimika.course.util.enums;

/**
 * Enumeration representing the various states of an assignment submission
 * Must match the database constraint: CHECK (status IN ('draft', 'submitted', 'graded', 'returned'))
 */
public enum SubmissionStatus {

    /**
     * Submission has been created but not yet submitted by student
     */
    DRAFT,

    /**
     * Submission has been submitted by student and is awaiting grading
     */
    SUBMITTED,

    /**
     * Submission has been graded and score/feedback provided
     */
    GRADED,

    /**
     * Submission has been returned to student for revision with feedback
     */
    RETURNED;

    /**
     * Check if this status indicates the submission is pending grading
     */
    public boolean isPendingGrading() {
        return this == SUBMITTED;
    }

    /**
     * Check if this status indicates the submission is completed
     */
    public boolean isCompleted() {
        return this == GRADED;
    }

    /**
     * Check if this status allows for revision
     */
    public boolean allowsRevision() {
        return this == DRAFT || this == RETURNED;
    }

    /**
     * Get user-friendly display name for the status
     */
    public String getDisplayName() {
        return switch (this) {
            case DRAFT -> "Draft";
            case SUBMITTED -> "Submitted";
            case GRADED -> "Graded";
            case RETURNED -> "Returned for Revision";
        };
    }

    /**
     * Get the database value (lowercase)
     */
    public String getDatabaseValue() {
        return this.name().toLowerCase();
    }

    /**
     * Create enum from database value
     */
    public static SubmissionStatus fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return valueOf(value.toUpperCase());
    }
}