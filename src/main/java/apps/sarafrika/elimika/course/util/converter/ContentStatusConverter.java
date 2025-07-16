package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the ContentStatus enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the ContentStatus enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class ContentStatusConverter implements AttributeConverter<ContentStatus, String> {

    /**
     * Converts the ContentStatus enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., ContentStatus.IN_REVIEW).
     * @return The string value for the database (e.g., "in_review"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(ContentStatus attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding ContentStatus enum.
     *
     * @param dbData The string value from the database (e.g., "in_review").
     * @return The matching ContentStatus enum, or null if the database value is null.
     */
    @Override
    public ContentStatus convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? ContentStatus.fromValue(dbData) : null;
    }
}
