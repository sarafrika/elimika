package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

/**
 * A guardian's decision on a minor's invitation.
 * <p>
 * Consenting does two things at once: it approves the child's affiliation with the
 * organisation, and it establishes the guardian's own ongoing visibility of the child's
 * learning at the chosen scope.
 */
@Schema(
        name = "GuardianConsentRequest",
        description = "Records a guardian's consent for a minor to join an organisation."
)
public record GuardianConsentRequestDTO(

        @Schema(description = "**[OPTIONAL]** How much of the child's learning the guardian will see. " +
                "Defaults to FULL.",
                example = "FULL", allowableValues = {"FULL", "ACADEMICS", "ATTENDANCE"}, nullable = true)
        @Pattern(regexp = "(?i)FULL|ACADEMICS|ATTENDANCE",
                message = "Share scope must be one of FULL, ACADEMICS or ATTENDANCE")
        @JsonProperty("share_scope")
        String shareScope,

        @Schema(description = "**[REQUIRED]** Confirms the guardian has seen what the organisation will " +
                "be able to view: the child's enrolment and performance in that institution's own " +
                "classes, and nothing beyond it.",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @AssertTrue(message = "The data-sharing scope must be acknowledged before consenting")
        @JsonProperty("scope_acknowledged")
        boolean scopeAcknowledged
) {
}
