package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** One reason a course was recommended. */
@Schema(name = "RecommendationReason", description = "Why a course was recommended")
public record RecommendationReasonDTO(

        @Schema(description = "Stable reason code: NEXT_STEP, PREREQUISITE_PENDING, CO_ENROLLED, CATEGORY, SKILL_GAP, "
                + "AFFILIATION, SIMILAR_CONTENT or POPULAR", example = "NEXT_STEP")
        @JsonProperty("code")
        String code,

        @Schema(description = "Display text", example = "Next step after Python Basics")
        @JsonProperty("text")
        String text,

        @Schema(description = "The course, category, organisation or instructor the reason refers to, if any")
        @JsonProperty("related_uuid")
        UUID relatedUuid

) {
}
