package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A course or programme an instructor may apply to train. {@code my_application} and
 * {@code minimum_training_fee} are read live from the database for the caller.
 */
@Schema(name = "ApplyCatalogueItem", description = "A course or programme in the apply-to-train catalogue.")
public record ApplyCatalogueItem(
        @Schema(allowableValues = {"course", "programme"}) @JsonProperty("type") String type,
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("code") String code,
        @JsonProperty("category_uuids") List<UUID> categoryUuids,
        @JsonProperty("category_names") List<String> categoryNames,
        @JsonProperty("creator_name") String creatorName,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @Schema(description = "A programme's is the band every member course accepts; null when unbounded")
        @JsonProperty("age_lower_limit") Integer ageLowerLimit,
        @JsonProperty("age_upper_limit") Integer ageUpperLimit,
        @Schema(description = "Active lessons; a programme's over its member courses")
        @JsonProperty("lesson_count") long lessonCount,
        @JsonProperty("requirement_count") long requirementCount,
        @Schema(description = "Member courses; null for a course") @JsonProperty("course_count") Integer courseCount,
        @Schema(description = "Shares at least one skill with the caller's skills wallet")
        @JsonProperty("matches_skills") boolean matchesSkills,
        @Schema(description = "The caller's application, or null when they have not applied")
        @JsonProperty("my_application") ApplyCatalogueApplication myApplication,
        @Schema(description = "The rate-card floor: a course's own minimum fee, a programme's highest across its courses")
        @JsonProperty("minimum_training_fee") BigDecimal minimumTrainingFee
) {
}
