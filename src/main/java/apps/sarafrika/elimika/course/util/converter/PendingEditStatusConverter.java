package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.PendingEditStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the PendingEditStatus enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 */
@Converter(autoApply = true)
public class PendingEditStatusConverter implements AttributeConverter<PendingEditStatus, String> {

    @Override
    public String convertToDatabaseColumn(PendingEditStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public PendingEditStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? PendingEditStatus.fromValue(dbData) : null;
    }
}
