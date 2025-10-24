package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseTrainingApplicationStatusConverter implements AttributeConverter<CourseTrainingApplicationStatus, String> {

    @Override
    public String convertToDatabaseColumn(CourseTrainingApplicationStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseTrainingApplicationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseTrainingApplicationStatus.fromValue(dbData);
    }
}
