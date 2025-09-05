package apps.sarafrika.elimika.notifications.util.converter;

import apps.sarafrika.elimika.notifications.model.UserNotificationPreferences.DigestMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the DigestMode enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the DigestMode enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class DigestModeConverter implements AttributeConverter<DigestMode, String> {

    /**
     * Converts the DigestMode enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., DigestMode.IMMEDIATE).
     * @return The string value for the database (e.g., "IMMEDIATE"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(DigestMode attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding DigestMode enum.
     *
     * @param dbData The string value from the database (e.g., "IMMEDIATE").
     * @return The matching DigestMode enum, or null if the database value is null.
     */
    @Override
    public DigestMode convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? DigestMode.fromValue(dbData) : null;
    }
}