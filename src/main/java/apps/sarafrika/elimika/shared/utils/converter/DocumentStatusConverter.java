package apps.sarafrika.elimika.shared.utils.converter;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class DocumentStatusConverter implements AttributeConverter<DocumentStatus, String> {

    @Override
    public String convertToDatabaseColumn(DocumentStatus attribute) {
        return attribute != null ? attribute.name().toUpperCase(Locale.ROOT) : null;
    }

    @Override
    public DocumentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return DocumentStatus.fromString(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
