package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.TrainingRequirementAcquisition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TrainingRequirementAcquisitionConverter implements AttributeConverter<TrainingRequirementAcquisition, String> {

    @Override
    public String convertToDatabaseColumn(TrainingRequirementAcquisition attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public TrainingRequirementAcquisition convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TrainingRequirementAcquisition.fromValue(dbData);
    }
}
