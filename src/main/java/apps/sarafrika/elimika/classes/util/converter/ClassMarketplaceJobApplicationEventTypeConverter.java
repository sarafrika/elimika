package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationEventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClassMarketplaceJobApplicationEventTypeConverter
        implements AttributeConverter<ClassMarketplaceJobApplicationEventType, String> {

    @Override
    public String convertToDatabaseColumn(ClassMarketplaceJobApplicationEventType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ClassMarketplaceJobApplicationEventType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ClassMarketplaceJobApplicationEventType.fromValue(dbData);
    }
}
