package apps.sarafrika.elimika.shared.utils.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateBasisTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record Priced(@JsonProperty("rate_basis") RateBasis rateBasis) {
    }

    @Test
    @DisplayName("a missing value is not quietly read as per hour")
    void nullIsNotPerHour() {
        assertThatThrownBy(() -> RateBasis.fromValue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("wire and stored spellings both resolve")
    void wireAndStoredSpellingsResolve() {
        assertThat(RateBasis.fromValue("per_session")).isEqualTo(RateBasis.PER_SESSION);
        assertThat(RateBasis.fromValue("PER_DAY")).isEqualTo(RateBasis.PER_DAY);
    }

    @Test
    @DisplayName("an omitted or null basis in JSON stays absent for the caller to decide")
    void jsonNullStaysAbsent() throws Exception {
        assertThat(objectMapper.readValue("{\"rate_basis\":null}", Priced.class).rateBasis()).isNull();
        assertThat(objectMapper.readValue("{}", Priced.class).rateBasis()).isNull();
        assertThat(objectMapper.readValue("{\"rate_basis\":\"per_day\"}", Priced.class).rateBasis())
                .isEqualTo(RateBasis.PER_DAY);
    }
}
