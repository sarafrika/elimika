package apps.sarafrika.elimika.instructor.util.converter;

import apps.sarafrika.elimika.instructor.util.enums.ProficiencyLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the ProficiencyLevel enum to its name string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the enum name() and valueOf() methods since ProficiencyLevel
 * stores enum names (BEGINNER, INTERMEDIATE, etc.) in the database.
 */
@Converter(autoApply = true)
public class ProficiencyLevelConverter implements AttributeConverter<ProficiencyLevel, String> {

    /**
     * Converts the ProficiencyLevel enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., ProficiencyLevel.BEGINNER).
     * @return The string value for the database (e.g., "BEGINNER"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(ProficiencyLevel attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding ProficiencyLevel enum.
     *
     * @param dbData The string value from the database (e.g., "BEGINNER").
     * @return The matching ProficiencyLevel enum, or null if the database value is null.
     */
    @Override
    public ProficiencyLevel convertToEntityAttribute(String dbData) {
        return dbData != null ? ProficiencyLevel.valueOf(dbData) : null;
    }
}