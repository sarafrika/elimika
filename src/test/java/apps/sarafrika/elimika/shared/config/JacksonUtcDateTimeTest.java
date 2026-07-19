package apps.sarafrika.elimika.shared.config;

import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the UTC wire contract configured by {@link JacksonConfig}: {@link LocalDateTime}
 * fields are serialized with an explicit {@code Z} and parsed back from zoned or zone-less
 * input, while {@link LocalDate} is left untouched.
 */
class JacksonUtcDateTimeTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Mirror Spring Boot's defaults (ISO strings, not numeric arrays) so this
        // exercises the same configuration the application runs with.
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        new JacksonConfig().utcDateTimeCustomizer().customize(builder);
        objectMapper = builder.build();
    }

    @Test
    void serializesLocalDateTimeAsUtcInstantWithZ() throws Exception {
        Holder holder = new Holder();
        holder.moment = LocalDateTime.of(2024, 1, 1, 9, 0, 0);

        assertThat(objectMapper.writeValueAsString(holder))
                .contains("\"moment\":\"2024-01-01T09:00:00Z\"");
    }

    @Test
    void leavesLocalDateWithoutAZoneMarker() throws Exception {
        Holder holder = new Holder();
        holder.day = LocalDate.of(2024, 1, 1);

        assertThat(objectMapper.writeValueAsString(holder))
                .contains("\"day\":\"2024-01-01\"");
    }

    @Test
    void deserializesZuluStringAsUtc() throws Exception {
        Holder holder = objectMapper.readValue("{\"moment\":\"2024-01-01T09:00:00Z\"}", Holder.class);
        assertThat(holder.moment).isEqualTo(LocalDateTime.of(2024, 1, 1, 9, 0, 0));
    }

    @Test
    void deserializesExplicitOffsetNormalizingToUtc() throws Exception {
        Holder holder = objectMapper.readValue("{\"moment\":\"2024-01-01T12:00:00+03:00\"}", Holder.class);
        assertThat(holder.moment).isEqualTo(LocalDateTime.of(2024, 1, 1, 9, 0, 0));
    }

    @Test
    void deserializesBareLocalStringAsUtc() throws Exception {
        Holder holder = objectMapper.readValue("{\"moment\":\"2024-01-01T09:00:00\"}", Holder.class);
        assertThat(holder.moment).isEqualTo(LocalDateTime.of(2024, 1, 1, 9, 0, 0));
    }

    @Test
    void responseEnvelopeTimestampCarriesZuluOffset() throws Exception {
        ResponseDTO<String> response = new ResponseDTO<>("ok", 200, "OK", null,
                LocalDateTime.of(2024, 1, 1, 9, 0, 0));

        assertThat(objectMapper.writeValueAsString(response))
                .contains("\"timestamp\":\"2024-01-01T09:00:00Z\"");
    }

    static class Holder {
        public LocalDateTime moment;
        public LocalDate day;
    }
}
