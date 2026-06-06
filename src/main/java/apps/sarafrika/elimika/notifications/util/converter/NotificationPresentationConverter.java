package apps.sarafrika.elimika.notifications.util.converter;

import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class NotificationPresentationConverter implements AttributeConverter<NotificationPresentation, String> {
    @Override
    public String convertToDatabaseColumn(NotificationPresentation attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public NotificationPresentation convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NotificationPresentation.fromDatabaseValue(dbData.toUpperCase(Locale.ROOT));
    }
}
