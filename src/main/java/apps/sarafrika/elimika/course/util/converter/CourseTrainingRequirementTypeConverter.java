package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingRequirementType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseTrainingRequirementTypeConverter implements AttributeConverter<CourseTrainingRequirementType, String> {

    @Override
    public String convertToDatabaseColumn(CourseTrainingRequirementType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseTrainingRequirementType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseTrainingRequirementType.fromValue(dbData);
    }
}
