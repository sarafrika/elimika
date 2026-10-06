package apps.sarafrika.elimika.profile.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

/** The profile basics, how many items each section holds, and how complete the profile is. */
@Schema(name = "ProfessionalProfileSummary", description = "Professional profile basics with per-section counts and completeness")
public record ProfileSummaryDTO(
        @JsonProperty("user_uuid") UUID userUuid,
        @JsonProperty("basics") ProfessionalProfileDTO basics,
        @Schema(description = "Item count per section, keyed by the section's path name (skills, education, ...)")
        @JsonProperty("section_counts") Map<String, Long> sectionCounts,
        @JsonProperty("verified_items") long verifiedItems,
        @JsonProperty("completed_sections") int completedSections,
        @JsonProperty("total_sections") int totalSections,
        @JsonProperty("completeness_percent") int completenessPercent
) {
}
