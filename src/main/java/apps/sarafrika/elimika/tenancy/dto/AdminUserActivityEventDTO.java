package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing an audit event related to a specific user dossier.
 */
@Schema(name = "AdminUserActivityEvent", description = "Represents a request audit event related to a specific user")
public record AdminUserActivityEventDTO(

        @Schema(description = "Unique identifier for the activity event")
        @JsonProperty("event_uuid")
        UUID eventUuid,

        @Schema(description = "When the activity occurred (UTC)")
        @JsonProperty("occurred_at")
        LocalDateTime occurredAt,

        @Schema(description = "Human-readable summary of the activity")
        @JsonProperty("summary")
        String summary,

        @Schema(description = "Audit category derived from the endpoint")
        @JsonProperty("category")
        String category,

        @Schema(description = "Whether the selected user performed the action, was targeted by it, or both")
        @JsonProperty("scope")
        String scope,

        @Schema(description = "HTTP method invoked by the request")
        @JsonProperty("http_method")
        String httpMethod,

        @Schema(description = "Endpoint path that was called")
        @JsonProperty("endpoint")
        String endpoint,

        @Schema(description = "Query string that accompanied the request")
        @JsonProperty("query")
        String query,

        @Schema(description = "Response status returned for the request")
        @JsonProperty("response_status")
        Integer responseStatus,

        @Schema(description = "Processing time in milliseconds")
        @JsonProperty("processing_time_ms")
        Long processingTimeMs,

        @Schema(description = "Actor name captured by the request audit log")
        @JsonProperty("actor_name")
        String actorName,

        @Schema(description = "Actor email captured by the request audit log")
        @JsonProperty("actor_email")
        String actorEmail,

        @Schema(description = "Actor user UUID captured by the request audit log")
        @JsonProperty("actor_uuid")
        UUID actorUuid,

        @Schema(description = "Actor domains captured during the request")
        @JsonProperty("actor_domains")
        String actorDomains,

        @Schema(description = "Selected dossier user UUID")
        @JsonProperty("target_user_uuid")
        UUID targetUserUuid,

        @Schema(description = "Related profile/entity type when derived from the endpoint")
        @JsonProperty("related_entity_type")
        String relatedEntityType,

        @Schema(description = "Related profile/entity UUID when derived from the endpoint")
        @JsonProperty("related_entity_uuid")
        UUID relatedEntityUuid,

        @Schema(description = "Unique request identifier captured by the audit trail")
        @JsonProperty("request_id")
        String requestId
) {
}
