package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.QuestionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the QuestionType enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the QuestionType enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class QuestionTypeConverter implements AttributeConverter<QuestionType, String> {

    /**
     * Converts the QuestionType enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., QuestionType.MULTIPLE_CHOICE).
     * @return The string value for the database (e.g., "multiple_choice"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(QuestionType attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding QuestionType enum.
     *
     * @param dbData The string value from the database (e.g., "multiple_choice").
     * @return The matching QuestionType enum, or null if the database value is null.
     */
    @Override
    public QuestionType convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? QuestionType.fromValue(dbData) : null;
    }
}