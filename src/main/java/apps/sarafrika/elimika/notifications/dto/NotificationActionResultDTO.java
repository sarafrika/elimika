package apps.sarafrika.elimika.notifications.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationActionResultDTO(
        @JsonProperty("action")
        String action,
        @JsonProperty("affected_count")
        int affectedCount
) {
}
