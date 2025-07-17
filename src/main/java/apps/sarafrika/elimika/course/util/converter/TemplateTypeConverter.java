package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.TemplateType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the TemplateType enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the TemplateType enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class TemplateTypeConverter implements AttributeConverter<TemplateType, String> {

    /**
     * Converts the TemplateType enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., TemplateType.COURSE_COMPLETION).
     * @return The string value for the database (e.g., "course_completion"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(TemplateType attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding TemplateType enum.
     *
     * @param dbData The string value from the database (e.g., "course_completion").
     * @return The matching TemplateType enum, or null if the database value is null.
     */
    @Override
    public TemplateType convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? TemplateType.fromValue(dbData) : null;
    }
}