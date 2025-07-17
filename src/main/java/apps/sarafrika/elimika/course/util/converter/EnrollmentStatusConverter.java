package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the EnrollmentStatus enum to its explicitly defined string value for the database,
 * and back to the enum from the database string.
 * This implementation uses the getValue() and fromValue() methods in the EnrollmentStatus enum
 * for a robust and decoupled conversion.
 */
@Converter(autoApply = true)
public class EnrollmentStatusConverter implements AttributeConverter<EnrollmentStatus, String> {

    /**
     * Converts the EnrollmentStatus enum to its corresponding string value.
     *
     * @param attribute The enum value from the entity (e.g., EnrollmentStatus.ACTIVE).
     * @return The string value for the database (e.g., "active"), or null if the enum is null.
     */
    @Override
    public String convertToDatabaseColumn(EnrollmentStatus attribute) {
        // Delegates conversion to the enum itself for better encapsulation.
        return attribute != null ? attribute.getValue() : null;
    }

    /**
     * Converts the string from the database back to the corresponding EnrollmentStatus enum.
     *
     * @param dbData The string value from the database (e.g., "active").
     * @return The matching EnrollmentStatus enum, or null if the database value is null.
     */
    @Override
    public EnrollmentStatus convertToEntityAttribute(String dbData) {
        // Uses the static factory method in the enum for safe and efficient lookup.
        return dbData != null ? EnrollmentStatus.fromValue(dbData) : null;
    }
}