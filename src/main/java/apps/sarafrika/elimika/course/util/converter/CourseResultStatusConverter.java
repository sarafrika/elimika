package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseResultStatusConverter implements AttributeConverter<CourseResultStatus, String> {

    @Override
    public String convertToDatabaseColumn(CourseResultStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseResultStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseResultStatus.fromValue(dbData);
    }
}
