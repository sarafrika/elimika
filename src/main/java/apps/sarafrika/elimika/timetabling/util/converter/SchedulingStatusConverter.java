package apps.sarafrika.elimika.timetabling.util.converter;

import apps.sarafrika.elimika.timetabling.util.enums.SchedulingStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for SchedulingStatus enum.
 * <p>
 * This converter handles the mapping between SchedulingStatus enum values and their database
 * string representations. It follows the project's pattern for enum converters with auto-apply
 * enabled for automatic usage across all entities with SchedulingStatus fields.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Converter(autoApply = true)
public class SchedulingStatusConverter implements AttributeConverter<SchedulingStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(SchedulingStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public SchedulingStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? SchedulingStatus.fromValue(dbData) : null;
    }
}