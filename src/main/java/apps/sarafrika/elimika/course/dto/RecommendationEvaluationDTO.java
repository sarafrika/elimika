package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Offline leave-last-out evaluation of the course recommenders. A regression gate, not a tuning target:
 * the data is small.
 */
@Schema(name = "RecommendationEvaluation", description = "Leave-last-out recall and coverage of the course recommenders")
public record RecommendationEvaluationDTO(

        @Schema(description = "Cut-off rank")
        @JsonProperty("k")
        int k,

        @Schema(description = "Learners with at least two enrolments whose latest enrolment is a public course")
        @JsonProperty("learners_evaluated")
        int learnersEvaluated,

        @Schema(description = "Public courses in the catalogue")
        @JsonProperty("catalogue_size")
        int catalogueSize,

        @Schema(description = "One row per model")
        @JsonProperty("models")
        List<ModelScore> models,

        @Schema(description = "When the evaluation ran (UTC)")
        @JsonProperty("evaluated_at")
        Instant evaluatedAt

) {

    /** How one model did. */
    @Schema(name = "RecommendationModelScore")
    public record ModelScore(
            @Schema(description = "rules-v2, popularity or legacy-newest")
            @JsonProperty("model") String model,

            @Schema(description = "Share of learners whose hidden enrolment is in the top k")
            @JsonProperty("recall_at_k") double recallAtK,

            @Schema(description = "Mean nDCG@k with one relevant item")
            @JsonProperty("ndcg_at_k") double ndcgAtK,

            @Schema(description = "Distinct courses recommended across learners / catalogue size")
            @JsonProperty("coverage") double coverage
    ) {
    }
}
