package apps.sarafrika.elimika.classes.util.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persists a small {@code List<UUID>} of foreign identifiers (e.g. target student groups) as a
 * single comma-separated column. Unparseable tokens are dropped on read so a hand-edited or
 * legacy row never fails entity hydration. Not auto-applied — opt in per field with
 * {@code @Convert} so it never clashes with other {@code List<UUID>} mappings.
 */
@Converter
public class UuidListCsvConverter implements AttributeConverter<List<UUID>, String> {

    @Override
    public String convertToDatabaseColumn(List<UUID> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream()
                .filter(java.util.Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.joining(","));
    }

    @Override
    public List<UUID> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        List<UUID> values = new ArrayList<>();
        for (String token : Arrays.stream(dbData.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList()) {
            try {
                values.add(UUID.fromString(token));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed identifiers rather than failing the whole row.
            }
        }
        return values;
    }
}
