package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CourseAssessmentLineItemRubricEvaluationStatusConverter implements AttributeConverter<CourseAssessmentLineItemRubricEvaluationStatus, String> {

    @Override
    public String convertToDatabaseColumn(CourseAssessmentLineItemRubricEvaluationStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseAssessmentLineItemRubricEvaluationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseAssessmentLineItemRubricEvaluationStatus.fromValue(dbData);
    }
}
