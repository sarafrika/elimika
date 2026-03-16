package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CourseAssessmentLineItemRubricEvaluationStatus {
    PENDING("pending"),
    COMPLETED("completed");

    private final String value;

    CourseAssessmentLineItemRubricEvaluationStatus(String value) {
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
    public static CourseAssessmentLineItemRubricEvaluationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CourseAssessmentLineItemRubricEvaluationStatus status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown CourseAssessmentLineItemRubricEvaluationStatus: " + value);
    }
}
