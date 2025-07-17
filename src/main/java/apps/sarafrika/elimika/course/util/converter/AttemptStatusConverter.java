package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the AttemptStatus enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the AttemptStatus enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class AttemptStatusConverter implements AttributeConverter<AttemptStatus, String> {

    /**
     * Converts the AttemptStatus enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., AttemptStatus.IN_PROGRESS).
     * @return The string value for the database (e.g., "in_progress"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(AttemptStatus attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding AttemptStatus enum.
     *
     * @param dbData The string value from the database (e.g., "in_progress").
     * @return The matching AttemptStatus enum, or null if the database value is null.
     */
    @Override
    public AttemptStatus convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? AttemptStatus.fromValue(dbData) : null;
    }
}