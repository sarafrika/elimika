package apps.sarafrika.elimika.timetabling.util.converter;

import apps.sarafrika.elimika.timetabling.util.enums.EnrollmentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for EnrollmentStatus enum.
 * <p>
 * This converter handles the mapping between EnrollmentStatus enum values and their database
 * string representations. It follows the project's pattern for enum converters with auto-apply
 * enabled for automatic usage across all entities with EnrollmentStatus fields.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Converter(autoApply = true)
public class EnrollmentStatusConverter implements AttributeConverter<EnrollmentStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(EnrollmentStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public EnrollmentStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? EnrollmentStatus.fromValue(dbData) : null;
    }
}