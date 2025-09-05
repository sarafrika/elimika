package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.RecurrenceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the RecurrenceType enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the RecurrenceType enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class RecurrenceTypeConverter implements AttributeConverter<RecurrenceType, String> {

    /**
     * Converts the RecurrenceType enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., RecurrenceType.WEEKLY).
     * @return The string value for the database (e.g., "WEEKLY"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(RecurrenceType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding RecurrenceType enum.
     *
     * @param dbData The string value from the database (e.g., "WEEKLY").
     * @return The matching RecurrenceType enum, or null if the database value is null.
     */
    @Override
    public RecurrenceType convertToEntityAttribute(String dbData) {
        return dbData != null ? RecurrenceType.fromValue(dbData) : null;
    }
}