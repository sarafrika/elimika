package apps.sarafrika.elimika.booking.util.converter;

import apps.sarafrika.elimika.shared.enums.LocationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BookingDeliveryModeConverter implements AttributeConverter<LocationType, String> {

    @Override
    public String convertToDatabaseColumn(LocationType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public LocationType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LocationType.fromValue(dbData);
    }
}
