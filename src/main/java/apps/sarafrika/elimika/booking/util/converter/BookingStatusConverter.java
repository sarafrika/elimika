package apps.sarafrika.elimika.booking.util.converter;

import apps.sarafrika.elimika.shared.enums.BookingStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class BookingStatusConverter implements AttributeConverter<BookingStatus, String> {

    @Override
    public String convertToDatabaseColumn(BookingStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue().toUpperCase(Locale.ROOT);
    }

    @Override
    public BookingStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return BookingStatus.fromValue(dbData.toLowerCase(Locale.ROOT));
    }
}
