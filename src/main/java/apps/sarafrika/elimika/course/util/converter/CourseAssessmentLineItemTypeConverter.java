package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseAssessmentLineItemTypeConverter
        implements AttributeConverter<CourseAssessmentLineItemType, String> {

    @Override
    public String convertToDatabaseColumn(CourseAssessmentLineItemType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public CourseAssessmentLineItemType convertToEntityAttribute(String dbData) {
        return dbData != null ? CourseAssessmentLineItemType.fromString(dbData) : null;
    }
}
