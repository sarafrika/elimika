package apps.sarafrika.elimika.booking.util.converter;

import apps.sarafrika.elimika.shared.enums.SessionFormat;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter
public class BookingTrainingFormatConverter implements AttributeConverter<SessionFormat, String> {

    @Override
    public String convertToDatabaseColumn(SessionFormat attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public SessionFormat convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SessionFormat.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
