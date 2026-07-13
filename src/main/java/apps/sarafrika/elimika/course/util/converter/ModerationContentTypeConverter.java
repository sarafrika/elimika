package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the ModerationContentType enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 */
@Converter(autoApply = true)
public class ModerationContentTypeConverter implements AttributeConverter<ModerationContentType, String> {

    @Override
    public String convertToDatabaseColumn(ModerationContentType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ModerationContentType convertToEntityAttribute(String dbData) {
        return dbData != null ? ModerationContentType.fromValue(dbData) : null;
    }
}
