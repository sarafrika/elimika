package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "AdminDomainApplication", description = "A domain request in the platform admin approval queue")
public record AdminDomainApplicationDTO(

        @JsonProperty("user_uuid")
        UUID userUuid,

        @JsonProperty("full_name")
        String fullName,

        @JsonProperty("email")
        String email,

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
