package apps.sarafrika.elimika.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseDTO<T>(
        T data,
        int status,
        String message,
        Map<String, String> errors
) {
}
