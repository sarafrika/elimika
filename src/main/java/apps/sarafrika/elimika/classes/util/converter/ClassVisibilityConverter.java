package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class ClassVisibilityConverter implements AttributeConverter<ClassVisibility, String> {

    @Override
    public String convertToDatabaseColumn(ClassVisibility attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ClassVisibility convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ClassVisibility.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
