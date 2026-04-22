package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class ClassMarketplaceJobApplicationStatusConverter implements AttributeConverter<ClassMarketplaceJobApplicationStatus, String> {

    @Override
    public String convertToDatabaseColumn(ClassMarketplaceJobApplicationStatus attribute) {
        return attribute == null ? null : attribute.getValue().toUpperCase(Locale.ROOT);
    }

    @Override
    public ClassMarketplaceJobApplicationStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ClassMarketplaceJobApplicationStatus.fromValue(dbData.toLowerCase(Locale.ROOT));
    }
}
