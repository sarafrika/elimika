package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.ClassRecurrenceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class ClassRecurrenceTypeConverter implements AttributeConverter<ClassRecurrenceType, String> {

    @Override
    public String convertToDatabaseColumn(ClassRecurrenceType attribute) {
        return attribute == null ? null : attribute.name().toUpperCase(Locale.ROOT);
    }

    @Override
    public ClassRecurrenceType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ClassRecurrenceType.fromValue(dbData.toUpperCase(Locale.ROOT));
    }
}
