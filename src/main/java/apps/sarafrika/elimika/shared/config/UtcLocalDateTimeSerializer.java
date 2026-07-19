package apps.sarafrika.elimika.shared.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Serializes {@link LocalDateTime} values as unambiguous UTC ISO-8601 strings
 * carrying an explicit {@code Z} offset (for example {@code 2024-01-01T09:00:00Z}).
 * <p>
 * Every persisted instant in Elimika is stored in UTC and the JVM runs in UTC
 * (see {@code DateTimeConfig}), so a bare {@code LocalDateTime} read back from the
 * database already represents a UTC wall-clock time. Emitting it without a zone
 * marker (the Jackson default) makes browsers parse it as <em>local</em> time,
 * silently shifting every timestamp by the viewer's offset. Appending the UTC
 * offset makes the wire contract explicit so clients can localize correctly.
 */
public class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.atOffset(ZoneOffset.UTC).format(FORMATTER));
    }

    @Override
    public Class<LocalDateTime> handledType() {
        return LocalDateTime.class;
    }
}
