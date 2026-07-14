package apps.sarafrika.elimika.resourcing.util.converter;

import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ResourceBookingStatusConverter implements AttributeConverter<ResourceBookingStatus, String> {

    @Override
    public String convertToDatabaseColumn(ResourceBookingStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ResourceBookingStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ResourceBookingStatus.fromValue(dbData) : null;
    }
}
