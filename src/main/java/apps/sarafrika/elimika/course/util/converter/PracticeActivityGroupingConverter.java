package apps.sarafrika.elimika.course.util.converter;

import apps.sarafrika.elimika.course.util.enums.PracticeActivityGrouping;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = true)
public class PracticeActivityGroupingConverter implements AttributeConverter<PracticeActivityGrouping, String> {

    @Override
    public String convertToDatabaseColumn(PracticeActivityGrouping attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public PracticeActivityGrouping convertToEntityAttribute(String dbData) {
        return dbData != null ? PracticeActivityGrouping.fromString(dbData.toUpperCase(Locale.ROOT)) : null;
    }
}
