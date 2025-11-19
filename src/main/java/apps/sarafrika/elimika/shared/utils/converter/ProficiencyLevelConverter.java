package apps.sarafrika.elimika.shared.utils.converter;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Attribute converter for storing {@link ProficiencyLevel} as normalized uppercase strings.
 * Using a converter keeps persistence consistent across instructor and course creator modules
 * and ensures legacy values are read case-insensitively.
 */
@Converter(autoApply = false)
public class ProficiencyLevelConverter implements AttributeConverter<ProficiencyLevel, String> {

    @Override
    public String convertToDatabaseColumn(ProficiencyLevel attribute) {
        return attribute != null ? attribute.name().toUpperCase(Locale.ROOT) : null;
    }

    @Override
    public ProficiencyLevel convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ProficiencyLevel.fromValue(dbData.trim());
    }
}
