package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CoursePrerequisiteType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class CoursePrerequisiteTypeConverter implements AttributeConverter<CoursePrerequisiteType, String> {

    @Override
    public String convertToDatabaseColumn(CoursePrerequisiteType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public CoursePrerequisiteType convertToEntityAttribute(String dbData) {
        return dbData != null ? CoursePrerequisiteType.fromString(dbData.toUpperCase(Locale.ROOT)) : null;
    }
}
