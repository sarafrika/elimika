package apps.sarafrika.elimika.resourcing.util.converter;

import apps.sarafrika.elimika.resourcing.spi.ResourceBookingSourceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ResourceBookingSourceTypeConverter implements AttributeConverter<ResourceBookingSourceType, String> {

    @Override
    public String convertToDatabaseColumn(ResourceBookingSourceType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ResourceBookingSourceType convertToEntityAttribute(String dbData) {
        return dbData != null ? ResourceBookingSourceType.fromValue(dbData) : null;
    }
}
