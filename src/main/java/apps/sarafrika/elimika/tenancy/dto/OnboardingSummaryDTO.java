package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.OnboardingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "OnboardingSummary", description = "Onboarding state of one domain the user holds")
public record OnboardingSummaryDTO(

        @Schema(example = "course_creator")
        @JsonProperty("domain")
        String domain,

        @Schema(description = "not_started | in_progress | submitted | approved | rejected | suspended")
        @JsonProperty("status")
        OnboardingStatus status,

        @JsonProperty("requires_approval")
        boolean requiresApproval,

        @JsonProperty("active")
        boolean active,

        @JsonProperty("steps_completed")
        int stepsCompleted,

        @JsonProperty("steps_total")
        int stepsTotal,

        @JsonProperty("ready_for_submission")
        boolean readyForSubmission,

        @JsonProperty("submitted_at")
        LocalDateTime submittedAt
) {

    public static OnboardingSummaryDTO of(OnboardingDTO onboarding) {
        return new OnboardingSummaryDTO(onboarding.domain(), onboarding.status(), onboarding.requiresApproval(),
                onboarding.active(), onboarding.stepsCompleted(), onboarding.stepsTotal(),
                onboarding.readyForSubmission(), onboarding.submittedAt());
    }
}
