package apps.sarafrika.elimika.coursecreator.dto;

import apps.sarafrika.elimika.coursecreator.util.enums.CourseCreatorVerificationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseCreatorOnboardingStateDTO(
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid,

        @JsonProperty("categories")
        List<CourseCreatorCategoryPreferenceDTO> categories,

        @JsonProperty("skills_wallet_sections_completed")
        int skillsWalletSectionsCompleted,

        @JsonProperty("skills_wallet_sections_total")
        int skillsWalletSectionsTotal,

        @JsonProperty("verification_status")
        CourseCreatorVerificationStatus verificationStatus,

        @JsonProperty("admin_verified")
        Boolean adminVerified,

        @JsonProperty("verification_requested_at")
        LocalDateTime verificationRequestedAt,

        @JsonProperty("submitted_at")
        LocalDateTime submittedAt,

        @JsonProperty("reviewed_at")
        LocalDateTime reviewedAt,

        @JsonProperty("review_reason")
        String reviewReason,

        @JsonProperty("ready_for_submission")
        boolean readyForSubmission
) {
}
