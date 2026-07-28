package apps.sarafrika.elimika.classes.util.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists a small {@code List<String>} of controlled, comma-free tokens (e.g. target
 * learner groups) as a single comma-separated column. Not auto-applied — opt in per field
 * with {@code @Convert} so it never clashes with other {@code List<String>} mappings.
 */
@Converter
public class StringListCsvConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }
}
