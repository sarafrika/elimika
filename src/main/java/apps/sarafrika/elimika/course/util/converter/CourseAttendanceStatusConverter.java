package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseAttendanceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CourseAttendanceStatusConverter implements AttributeConverter<CourseAttendanceStatus, String> {

    @Override
    public String convertToDatabaseColumn(CourseAttendanceStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseAttendanceStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseAttendanceStatus.fromValue(dbData);
    }
}
