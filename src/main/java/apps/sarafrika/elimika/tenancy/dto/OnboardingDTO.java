package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.shared.spi.onboarding.OnboardingStep;
import apps.sarafrika.elimika.tenancy.util.enums.OnboardingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "Onboarding", description = "A user's onboarding for one domain: ordered steps, progress and review state")
public record OnboardingDTO(

        @Schema(example = "instructor")
        @JsonProperty("domain")
        String domain,

        @Schema(description = "not_started | in_progress | submitted | approved | rejected | suspended")
        @JsonProperty("status")
        OnboardingStatus status,

        @Schema(description = "False when the user has not requested this domain yet (the steps are a preview)")
        @JsonProperty("requested")
        boolean requested,

        @JsonProperty("requires_approval")
        boolean requiresApproval,

        @Schema(description = "True when the domain grants access now")
        @JsonProperty("active")
        boolean active,

        @JsonProperty("steps")
        List<OnboardingStep> steps,

        @JsonProperty("steps_completed")
        int stepsCompleted,

        @JsonProperty("steps_total")
        int stepsTotal,

        @JsonProperty("ready_for_submission")
        boolean readyForSubmission,

        @JsonProperty("submitted_at")
        LocalDateTime submittedAt,

        @JsonProperty("reviewed_at")
        LocalDateTime reviewedAt,

        @JsonProperty("review_reason")
        String reviewReason
) {
}
