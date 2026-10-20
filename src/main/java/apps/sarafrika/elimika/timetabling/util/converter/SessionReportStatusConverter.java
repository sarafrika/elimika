package apps.sarafrika.elimika.timetabling.util.converter;

import apps.sarafrika.elimika.timetabling.spi.SessionReportStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SessionReportStatusConverter implements AttributeConverter<SessionReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(SessionReportStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public SessionReportStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? SessionReportStatus.fromValue(dbData) : null;
    }
}
