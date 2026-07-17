package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload used by platform admins when moderating a course or training program.
 * <p>
 * Replaces the previous free-form {@code ?action=} query parameter so the allowed values are
 * expressed in the schema rather than validated by hand in the controller.
 */
@Schema(
        name = "ContentModerationDecisionRequest",
        description = """
                Payload for a moderation decision on a course or training program.

                When the content has a pending edit awaiting review, `approved` promotes that
                edit onto the live content and `rejected` discards it, leaving the live content
                untouched. Otherwise the decision applies to the content's own approval state.
                """,
        example = """
        {
          "action": "rejected",
          "reason": "Lesson 3 video is missing captions."
        }
        """
)
public record ContentModerationDecisionRequest(

        @Schema(
                description = "The decision to apply.",
                allowableValues = {"approved", "rejected", "revoked"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @JsonProperty("action")
        @NotNull(message = "Moderation action is required")
        ModerationAction action,

        @Schema(
                description = "Reason for the decision. Shown to the course creator and retained in the moderation history.",
                maxLength = 2000,
                nullable = true
        )
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("reason")
        @Size(max = 2000, message = "Reason must not exceed 2000 characters")
        String reason
) {
}
