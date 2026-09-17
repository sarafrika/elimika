package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TrainingApplicationEventTypeConverter implements AttributeConverter<TrainingApplicationEventType, String> {

    @Override
    public String convertToDatabaseColumn(TrainingApplicationEventType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TrainingApplicationEventType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TrainingApplicationEventType.fromValue(dbData);
    }
}
