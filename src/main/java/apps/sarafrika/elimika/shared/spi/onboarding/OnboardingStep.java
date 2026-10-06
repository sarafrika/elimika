package apps.sarafrika.elimika.shared.spi.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/** One onboarding step; {@code shared} steps hold user-owned data every domain reuses. */
@Schema(name = "OnboardingStep", description = "One onboarding step and whether it is done")
public record OnboardingStep(
        @JsonIgnore int position,
        @JsonProperty("key") String key,
        @JsonProperty("title") String title,
        @JsonProperty("required") boolean required,
        @JsonProperty("complete") boolean complete,
        @Schema(description = "True when the step's data belongs to the user and is reused by every domain they hold")
        @JsonProperty("shared") boolean shared,
        @Schema(description = "What is still missing, e.g. phone_number, bio, skills, CERTIFICATE_OF_REGISTRATION")
        @JsonProperty("missing") List<String> missing,
        @Schema(description = "Optional counts behind the step, e.g. items per skills wallet section")
        @JsonProperty("counts") Map<String, Long> counts
) {

    public OnboardingStep {
        missing = missing == null ? List.of() : List.copyOf(missing);
        counts = counts == null ? Map.of() : Map.copyOf(counts);
    }

    /** A step that is complete when nothing is missing. */
    public static OnboardingStep of(int position, String key, String title, boolean required, boolean shared,
                                    List<String> missing) {
        return new OnboardingStep(position, key, title, required, missing == null || missing.isEmpty(), shared,
                missing, null);
    }

    public OnboardingStep withCounts(Map<String, Long> stepCounts) {
        return new OnboardingStep(position, key, title, required, complete, shared, missing, stepCounts);
    }
}
