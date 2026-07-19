package apps.sarafrika.elimika.shared.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Parses incoming ISO-8601 date-time strings into {@link LocalDateTime}, tolerating
 * both zoned and zone-less input so the API accepts what clients now send.
 * <p>
 * The counterpart {@link UtcLocalDateTimeSerializer} emits values with a {@code Z}
 * offset, and clients echo those back. A request body may therefore contain:
 * <ul>
 *     <li>{@code 2024-01-01T09:00:00Z} or {@code 2024-01-01T12:00:00+03:00} — the
 *     instant is normalized to UTC before dropping the offset;</li>
 *     <li>{@code 2024-01-01T09:00:00} — assumed to already be UTC (the platform
 *     contract), parsed as-is.</li>
 * </ul>
 * This keeps the round-trip lossless while remaining backward compatible with any
 * caller still sending bare local date-times.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        final String trimmed = text.trim();
        try {
            // Handles trailing Z and explicit offsets, normalizing to UTC.
            return OffsetDateTime.parse(trimmed).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            // No offset present: treat as an already-UTC wall-clock time.
            return LocalDateTime.parse(trimmed);
        }
    }

    @Override
    public Class<?> handledType() {
        return LocalDateTime.class;
    }
}
