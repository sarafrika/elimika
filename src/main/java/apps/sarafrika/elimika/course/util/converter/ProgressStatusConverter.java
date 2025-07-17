package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.ProgressStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the ProgressStatus enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the ProgressStatus enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class ProgressStatusConverter implements AttributeConverter<ProgressStatus, String> {

    /**
     * Converts the ProgressStatus enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., ProgressStatus.IN_PROGRESS).
     * @return The string value for the database (e.g., "in_progress"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(ProgressStatus attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding ProgressStatus enum.
     *
     * @param dbData The string value from the database (e.g., "in_progress").
     * @return The matching ProgressStatus enum, or null if the database value is null.
     */
    @Override
    public ProgressStatus convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? ProgressStatus.fromValue(dbData) : null;
    }
}