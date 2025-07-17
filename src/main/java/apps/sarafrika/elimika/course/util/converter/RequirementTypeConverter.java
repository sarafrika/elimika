package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.RequirementType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the RequirementType enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the RequirementType enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class RequirementTypeConverter implements AttributeConverter<RequirementType, String> {

    /**
     * Converts the RequirementType enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., RequirementType.STUDENT).
     * @return The string value for the database (e.g., "student"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(RequirementType attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding RequirementType enum.
     *
     * @param dbData The string value from the database (e.g., "student").
     * @return The matching RequirementType enum, or null if the database value is null.
     */
    @Override
    public RequirementType convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? RequirementType.fromValue(dbData) : null;
    }
}