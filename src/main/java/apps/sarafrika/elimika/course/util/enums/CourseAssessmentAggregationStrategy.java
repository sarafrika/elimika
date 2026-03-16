package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CourseAssessmentAggregationStrategy {
    POINTS_SUM("points_sum"),
    WEIGHTED_AVERAGE("weighted_average");

    private final String value;

    CourseAssessmentAggregationStrategy(String value) {
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
    public static CourseAssessmentAggregationStrategy fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CourseAssessmentAggregationStrategy strategy : values()) {
            if (strategy.value.equals(normalized)) {
                return strategy;
            }
        }

        throw new IllegalArgumentException("Unknown CourseAssessmentAggregationStrategy: " + value);
    }

    public static CourseAssessmentAggregationStrategy fromString(String value) {
        return fromValue(value);
    }
}
