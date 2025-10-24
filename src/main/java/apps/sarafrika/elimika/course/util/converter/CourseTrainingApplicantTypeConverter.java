package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseTrainingApplicantTypeConverter implements AttributeConverter<CourseTrainingApplicantType, String> {

    @Override
    public String convertToDatabaseColumn(CourseTrainingApplicantType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseTrainingApplicantType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseTrainingApplicantType.fromValue(dbData);
    }
}
