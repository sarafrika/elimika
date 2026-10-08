package apps.sarafrika.elimika.perf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(name = "RumBatchRequest", description = "A batch of up to 50 real-user performance samples")
public record RumBatchRequest(
        @JsonProperty("events")
        @NotEmpty @Size(max = RumBatchRequest.MAX_EVENTS)
        List<@Valid @NotNull RumEventRequest> events
) {
    public static final int MAX_EVENTS = 50;
}
