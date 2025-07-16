package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ContentStatusConverter implements AttributeConverter<ContentStatus, String> {

    @Override
    public String convertToDatabaseColumn(ContentStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ContentStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ContentStatus.fromValue(dbData) : null;
    }
}