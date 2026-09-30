package apps.sarafrika.elimika.shared.tracking.entity;

import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryEventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = false)
public class DiscoveryEventTypeConverter implements AttributeConverter<DiscoveryEventType, String> {

    @Override
    public String convertToDatabaseColumn(DiscoveryEventType attribute) {
        return attribute == null ? null : attribute.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public DiscoveryEventType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DiscoveryEventType.fromValue(dbData);
    }
}
