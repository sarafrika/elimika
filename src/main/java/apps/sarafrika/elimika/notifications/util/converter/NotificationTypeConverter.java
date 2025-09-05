package apps.sarafrika.elimika.notifications.util.converter;

import apps.sarafrika.elimika.notifications.api.NotificationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the NotificationType enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the NotificationType enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    /**
     * Converts the NotificationType enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., NotificationType.COURSE_ENROLLMENT_WELCOME).
     * @return The string value for the database (e.g., "COURSE_ENROLLMENT_WELCOME"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding NotificationType enum.
     *
     * @param dbData The string value from the database (e.g., "COURSE_ENROLLMENT_WELCOME").
     * @return The matching NotificationType enum, or null if the database value is null.
     */
    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? NotificationType.fromValue(dbData) : null;
    }
}