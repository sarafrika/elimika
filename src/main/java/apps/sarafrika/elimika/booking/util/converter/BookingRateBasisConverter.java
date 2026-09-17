package apps.sarafrika.elimika.booking.util.converter;

import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter
public class BookingRateBasisConverter implements AttributeConverter<RateBasis, String> {

    @Override
    public String convertToDatabaseColumn(RateBasis attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RateBasis convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RateBasis.fromValue(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
