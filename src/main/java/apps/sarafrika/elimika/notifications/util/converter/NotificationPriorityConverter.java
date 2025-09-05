package apps.sarafrika.elimika.notifications.util.converter;

import apps.sarafrika.elimika.notifications.api.NotificationPriority;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the NotificationPriority enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the NotificationPriority enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class NotificationPriorityConverter implements AttributeConverter<NotificationPriority, String> {

    /**
     * Converts the NotificationPriority enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., NotificationPriority.HIGH).
     * @return The string value for the database (e.g., "HIGH"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(NotificationPriority attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding NotificationPriority enum.
     *
     * @param dbData The string value from the database (e.g., "HIGH").
     * @return The matching NotificationPriority enum, or null if the database value is null.
     */
    @Override
    public NotificationPriority convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? NotificationPriority.fromValue(dbData) : null;
    }
}