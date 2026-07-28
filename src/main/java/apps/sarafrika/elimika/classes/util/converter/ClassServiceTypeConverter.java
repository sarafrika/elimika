package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.shared.enums.ClassServiceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class ClassServiceTypeConverter implements AttributeConverter<ClassServiceType, String> {

    @Override
    public String convertToDatabaseColumn(ClassServiceType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ClassServiceType convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank()
                ? null
                : ClassServiceType.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
    }
}
