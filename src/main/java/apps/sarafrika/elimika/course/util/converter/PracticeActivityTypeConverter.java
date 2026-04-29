package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.PracticeActivityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class PracticeActivityTypeConverter implements AttributeConverter<PracticeActivityType, String> {

    @Override
    public String convertToDatabaseColumn(PracticeActivityType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public PracticeActivityType convertToEntityAttribute(String dbData) {
        return dbData != null ? PracticeActivityType.fromString(dbData.toUpperCase(Locale.ROOT)) : null;
    }
}
