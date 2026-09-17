package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TrainingApplicationTypeConverter implements AttributeConverter<TrainingApplicationType, String> {

    @Override
    public String convertToDatabaseColumn(TrainingApplicationType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TrainingApplicationType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TrainingApplicationType.fromValue(dbData);
    }
}
