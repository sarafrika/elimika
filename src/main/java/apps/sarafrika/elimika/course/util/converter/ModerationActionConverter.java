package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the ModerationAction enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 */
@Converter(autoApply = true)
public class ModerationActionConverter implements AttributeConverter<ModerationAction, String> {

    @Override
    public String convertToDatabaseColumn(ModerationAction attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ModerationAction convertToEntityAttribute(String dbData) {
        return dbData != null ? ModerationAction.fromValue(dbData) : null;
    }
}
