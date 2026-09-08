package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public attributes of a course, for a course page served to any caller. Richer than the
 * catalogue's card projection; carries none of the commercial terms either.
 */
@Schema(
        name = "PublicCourseProfile",
        description = "Public attributes of a course, for a course page served to any caller. "
                + "Carries no commercial terms."
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicCourseProfileDTO(

        @Schema(description = "Display title.", example = "Piano Foundations for Beginners")
        @JsonProperty("name")
        String name,

        @Schema(description = "Rich-text blurb, as authored.")
        @JsonProperty("description")
        String description,

        @Schema(description = "What a learner will be able to do afterwards.")
        @JsonProperty("objectives")
        String objectives,

        @Schema(description = "What a learner needs before starting.")
        @JsonProperty("prerequisites")
        String prerequisites,

        @Schema(description = "Public URL for the course thumbnail.")
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @Schema(description = "Public URL for the course banner.")
        @JsonProperty("banner_url")
        String bannerUrl,

        @Schema(description = "Public URL for the free intro video.")
        @JsonProperty("intro_video_url")
        String introVideoUrl,

        @Schema(description = "Hours component of the advertised duration.", example = "12")
        @JsonProperty("duration_hours")
        Integer durationHours,

        @Schema(description = "Minutes component of the advertised duration.", example = "30")
        @JsonProperty("duration_minutes")
        Integer durationMinutes,

        @Schema(description = "Disciplines the course is filed under.")
        @JsonProperty("category_names")
        List<String> categoryNames,

        @Schema(description = "List price. The same figure the catalogue entry sells at.", example = "6500.00")
        @JsonProperty("price")
        BigDecimal price,

        @Schema(description = "Maximum learners in one class, or null when uncapped.", example = "24")
        @JsonProperty("class_limit")
        Integer classLimit,

        @Schema(description = "Lower age bound, or null when unrestricted.", example = "8")
        @JsonProperty("age_lower_limit")
        Integer ageLowerLimit,

        @Schema(description = "Upper age bound, or null when unrestricted.", example = "14")
        @JsonProperty("age_upper_limit")
        Integer ageUpperLimit,

        @Schema(description = "Whether the course itself is published.", example = "true")
        @JsonProperty("published")
        boolean published,

        @Schema(description = "Whether the course can currently be enrolled on.", example = "true")
        @JsonProperty("accepts_new_enrollments")
        boolean acceptsNewEnrollments,

        @Schema(description = "The course creator.")
        @JsonProperty("creator_uuid")
        UUID creatorUuid,

        @Schema(description = "The creator's display name, resolved so the caller needs no second lookup.")
        @JsonProperty("creator_name")
        String creatorName,

        @Schema(description = "What a trainer must supply to deliver this course.")
        @JsonProperty("training_requirements")
        List<PublicCourseTrainingRequirement> trainingRequirements,

        @Schema(description = "When the course was last changed.")
        @JsonProperty("updated_date")
        String updatedDate
) {

    /** One thing a trainer must supply. No cost or provider commercials. */
    @Schema(name = "PublicCourseTrainingRequirement")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PublicCourseTrainingRequirement(

            @Schema(description = "What is needed.", example = "Upright piano")
            @JsonProperty("name")
            String name,

            @Schema(description = "Free-text detail.")
            @JsonProperty("description")
            String description,

            @Schema(description = "How many.", example = "1")
            @JsonProperty("quantity")
            Integer quantity,

            @Schema(description = "Unit the quantity is counted in.", example = "per class")
            @JsonProperty("unit")
            String unit,

            @Schema(description = "What kind of thing this is.", example = "equipment")
            @JsonProperty("requirement_type")
            String requirementType,

            @Schema(description = "Who supplies it.", example = "instructor")
            @JsonProperty("provided_by")
            String providedBy,

            @Schema(description = "Whether delivery depends on it.", example = "true")
            @JsonProperty("is_mandatory")
            Boolean isMandatory
    ) {
    }
}
