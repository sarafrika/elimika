package apps.sarafrika.elimika.profile.internal.converter;

import apps.sarafrika.elimika.profile.spi.AchievementType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AchievementTypeConverter implements AttributeConverter<AchievementType, String> {

    @Override
    public String convertToDatabaseColumn(AchievementType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AchievementType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AchievementType.fromValue(dbData);
    }
}
