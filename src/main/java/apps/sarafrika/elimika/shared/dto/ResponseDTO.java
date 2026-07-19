package apps.sarafrika.elimika.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseDTO<T>(
        T data,

        int status,

        String message,

        Map<String, String> errors,

        // Serialized as a UTC ISO-8601 instant (e.g. 2024-01-01T09:00:00Z) via the
        // global LocalDateTime serializer configured in JacksonConfig.
        LocalDateTime timestamp
) {
}
