package apps.sarafrika.elimika.profile.internal.converter;

import apps.sarafrika.elimika.profile.spi.ExperienceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ExperienceTypeConverter implements AttributeConverter<ExperienceType, String> {

    @Override
    public String convertToDatabaseColumn(ExperienceType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ExperienceType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ExperienceType.fromValue(dbData);
    }
}
