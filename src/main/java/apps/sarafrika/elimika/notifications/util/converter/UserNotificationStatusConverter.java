package apps.sarafrika.elimika.notifications.util.converter;

import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class UserNotificationStatusConverter implements AttributeConverter<UserNotificationStatus, String> {
    @Override
    public String convertToDatabaseColumn(UserNotificationStatus attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public UserNotificationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UserNotificationStatus.fromDatabaseValue(dbData.toUpperCase(Locale.ROOT));
    }
}
