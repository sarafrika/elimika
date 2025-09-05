package apps.sarafrika.elimika.availability.util.converter;

import apps.sarafrika.elimika.availability.util.enums.AvailabilityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AvailabilityTypeConverter implements AttributeConverter<AvailabilityType, String> {
    
    @Override
    public String convertToDatabaseColumn(AvailabilityType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public AvailabilityType convertToEntityAttribute(String dbData) {
        return dbData != null ? AvailabilityType.fromValue(dbData) : null;
    }
}