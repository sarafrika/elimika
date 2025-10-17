package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingRequirementProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CourseTrainingRequirementProviderConverter implements AttributeConverter<CourseTrainingRequirementProvider, String> {

    @Override
    public String convertToDatabaseColumn(CourseTrainingRequirementProvider attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CourseTrainingRequirementProvider convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CourseTrainingRequirementProvider.fromValue(dbData);
    }
}
