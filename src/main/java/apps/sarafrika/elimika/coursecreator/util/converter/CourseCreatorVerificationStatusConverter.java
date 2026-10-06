package apps.sarafrika.elimika.coursecreator.util.converter;

import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseCreatorVerificationStatusConverter implements AttributeConverter<CourseCreatorVerificationStatus, String> {

    @Override
    public String convertToDatabaseColumn(CourseCreatorVerificationStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseCreatorVerificationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseCreatorVerificationStatus.fromValue(dbData);
    }
}
