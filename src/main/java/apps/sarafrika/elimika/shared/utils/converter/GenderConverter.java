package apps.sarafrika.elimika.shared.utils.converter;

import apps.sarafrika.elimika.shared.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

/**
 * Stores {@link Gender} as its uppercase name and reads legacy values case-insensitively.
 */
@Converter(autoApply = false)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        return attribute != null ? attribute.name().toUpperCase(Locale.ROOT) : null;
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return Gender.fromString(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
