package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a single admin activity event in the dashboard feed.
 */
@Schema(name = "AdminActivityEvent", description = "Represents a recent administrative action recorded by the platform")
public record AdminActivityEventDTO(

        @Schema(description = "Unique identifier for the activity event")
        @JsonProperty("event_uuid")
        UUID eventUuid,

        @Schema(description = "When the activity occurred (UTC)")
        @JsonProperty("occurred_at")
        LocalDateTime occurredAt,

        @Schema(description = "Human-readable summary of the activity")
        @JsonProperty("summary")
        String summary,

        @Schema(description = "HTTP method invoked by the admin request")
        @JsonProperty("http_method")
        String httpMethod,

        @Schema(description = "Endpoint path that was called")
        @JsonProperty("endpoint")
        String endpoint,

        @Schema(description = "Query string (if any) that accompanied the request")
        @JsonProperty("query")
        String query,

        @Schema(description = "Response status returned for the request")
        @JsonProperty("response_status")
        Integer responseStatus,

        @Schema(description = "Processing time in milliseconds for the request")
        @JsonProperty("processing_time_ms")
        Long processingTimeMs,

        @Schema(description = "Administrator who performed the action (full name if available)")
        @JsonProperty("actor_name")
        String actorName,

        @Schema(description = "Email address of the administrator who performed the action")
        @JsonProperty("actor_email")
        String actorEmail,

        @Schema(description = "UUID of the administrator, if authenticated")
        @JsonProperty("actor_uuid")
        UUID actorUuid,

        @Schema(description = "Comma-separated list of domains attached to the administrator during the request")
        @JsonProperty("actor_domains")
        String actorDomains,

        @Schema(description = "Unique request identifier captured by the audit trail")
        @JsonProperty("request_id")
        String requestId
) {
}
