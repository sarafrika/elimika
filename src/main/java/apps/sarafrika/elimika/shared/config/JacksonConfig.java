package apps.sarafrika.elimika.shared.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.TimeZone;

/**
 * Centralizes JSON date-time handling so every timestamp Elimika emits is an
 * unambiguous UTC instant and every timestamp it accepts is normalized to UTC.
 * <p>
 * The overwhelming majority of entities and DTOs expose {@link LocalDateTime}
 * fields. Left to Jackson's defaults these serialize without a zone marker, which
 * clients (browsers in particular) misread as local time. Here we override just the
 * {@link LocalDateTime} (de)serializers — {@link java.time.LocalDate},
 * {@link java.time.LocalTime} and {@link java.time.OffsetDateTime} keep their
 * default handling — and pin the mapper's time zone to UTC.
 *
 * @see UtcLocalDateTimeSerializer
 * @see FlexibleLocalDateTimeDeserializer
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcDateTimeCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone("UTC"));
            builder.serializerByType(LocalDateTime.class, new UtcLocalDateTimeSerializer());
            builder.deserializerByType(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
        };
    }
}
