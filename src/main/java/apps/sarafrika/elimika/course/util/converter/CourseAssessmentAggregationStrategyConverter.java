package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseAssessmentAggregationStrategyConverter
        implements AttributeConverter<CourseAssessmentAggregationStrategy, String> {

    @Override
    public String convertToDatabaseColumn(CourseAssessmentAggregationStrategy attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public CourseAssessmentAggregationStrategy convertToEntityAttribute(String dbData) {
        return dbData != null ? CourseAssessmentAggregationStrategy.fromString(dbData) : null;
    }
}
