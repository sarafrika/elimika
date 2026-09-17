package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TrainingRateUpdateStatusConverter implements AttributeConverter<TrainingRateUpdateStatus, String> {

    @Override
    public String convertToDatabaseColumn(TrainingRateUpdateStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TrainingRateUpdateStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TrainingRateUpdateStatus.fromValue(dbData);
    }
}
