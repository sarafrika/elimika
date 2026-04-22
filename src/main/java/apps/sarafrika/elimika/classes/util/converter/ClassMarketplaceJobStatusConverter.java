package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class ClassMarketplaceJobStatusConverter implements AttributeConverter<ClassMarketplaceJobStatus, String> {

    @Override
    public String convertToDatabaseColumn(ClassMarketplaceJobStatus attribute) {
        return attribute == null ? null : attribute.getValue().toUpperCase(Locale.ROOT);
    }

    @Override
    public ClassMarketplaceJobStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ClassMarketplaceJobStatus.fromValue(dbData.toLowerCase(Locale.ROOT));
    }
}
