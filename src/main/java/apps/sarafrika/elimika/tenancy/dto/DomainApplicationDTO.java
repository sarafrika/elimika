package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "DomainApplication", description = "A domain a user holds or has asked for, with its approval state")
public record DomainApplicationDTO(

        @JsonProperty("user_uuid")
        UUID userUuid,

        @Schema(example = "course_creator")
        @JsonProperty("domain")
        String domain,

        @JsonProperty("status")
        DomainApprovalStatus status,

        @JsonProperty("requested_at")
        LocalDateTime requestedAt,

        @JsonProperty("reviewed_at")
        LocalDateTime reviewedAt,

        @JsonProperty("review_reason")
        String reviewReason
) {
}
