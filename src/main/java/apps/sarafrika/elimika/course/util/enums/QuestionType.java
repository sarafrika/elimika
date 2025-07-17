package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration representing the different types of quiz questions
 * Must match the database constraint: CHECK (question_type IN ('multiple_choice', 'true_false', 'short_answer', 'essay'))
 */
public enum QuestionType {
    MULTIPLE_CHOICE("multiple_choice"),
    TRUE_FALSE("true_false"),
    SHORT_ANSWER("short_answer"),
    ESSAY("essay");

    private final String value;
    private static final Map<String, QuestionType> VALUE_MAP = new HashMap<>();

    static {
        for (QuestionType type : QuestionType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toUpperCase(), type);
        }
    }

    QuestionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator
    public static QuestionType fromValue(String value) {
        QuestionType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown QuestionType: " + value);
        }
        return type;
    }

    public static QuestionType fromString(String value) {
        return fromValue(value);
    }

    /**
     * Get user-friendly display name for the question type
     */
    public String getDisplayName() {
        return switch (this) {
            case MULTIPLE_CHOICE -> "Multiple Choice";
            case TRUE_FALSE -> "True/False";
            case SHORT_ANSWER -> "Short Answer";
            case ESSAY -> "Essay";
        };
    }

    /**
     * Check if this question type has predefined answer options
     */
    public boolean hasOptions() {
        return this == MULTIPLE_CHOICE || this == TRUE_FALSE;
    }

    /**
     * Check if this question type requires text input from students
     */
    public boolean requiresTextInput() {
        return this == SHORT_ANSWER || this == ESSAY;
    }

    /**
     * Check if this question type can be auto-graded
     */
    public boolean isAutoGradable() {
        return this == MULTIPLE_CHOICE || this == TRUE_FALSE;
    }

    /**
     * Check if this question type requires manual grading
     */
    public boolean requiresManualGrading() {
        return this == SHORT_ANSWER || this == ESSAY;
    }

    /**
     * Check if this question type is objective (has definitive correct answers)
     */
    public boolean isObjective() {
        return this == MULTIPLE_CHOICE || this == TRUE_FALSE || this == SHORT_ANSWER;
    }

    /**
     * Check if this question type is subjective (open to interpretation)
     */
    public boolean isSubjective() {
        return this == ESSAY;
    }

    /**
     * Get the typical number of answer options for this question type
     */
    public int getTypicalOptionCount() {
        return switch (this) {
            case MULTIPLE_CHOICE -> 4; // A, B, C, D
            case TRUE_FALSE -> 2;      // True, False
            case SHORT_ANSWER, ESSAY -> 0; // No options
        };
    }

    /**
     * Get the recommended character limit for answers
     */
    public int getRecommendedCharacterLimit() {
        return switch (this) {
            case MULTIPLE_CHOICE, TRUE_FALSE -> 0; // No text input
            case SHORT_ANSWER -> 500;
            case ESSAY -> 5000;
        };
    }

    /**
     * Get the database value (same as getValue())
     */
    public String getDatabaseValue() {
        return this.value;
    }

    /**
     * Create enum from database value (same as fromValue())
     */
    public static QuestionType fromDatabaseValue(String value) {
        return fromValue(value);
    }
}