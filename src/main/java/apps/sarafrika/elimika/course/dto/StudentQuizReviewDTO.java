package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "StudentQuizReview",
        description = "Post-grading student quiz review with submitted answers and correct answers"
)
public record StudentQuizReviewDTO(

        @JsonProperty("quiz_uuid")
        UUID quizUuid,

        @JsonProperty("attempt_uuid")
        UUID attemptUuid,

        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @JsonProperty("status")
        AttemptStatus status,

        @JsonProperty("score")
        BigDecimal score,

        @JsonProperty("max_score")
        BigDecimal maxScore,

        @JsonProperty("percentage")
        BigDecimal percentage,

        @JsonProperty("is_passed")
        Boolean isPassed,

        @JsonProperty("questions")
        List<QuestionReviewDTO> questions
) {

    public record QuestionReviewDTO(

            @JsonProperty("uuid")
            UUID uuid,

            @JsonProperty("quiz_uuid")
            UUID quizUuid,

            @JsonProperty("question_text")
            String questionText,

            @JsonProperty("question_type")
            QuestionType questionType,

            @JsonProperty("points")
            BigDecimal points,

            @JsonProperty("display_order")
            Integer displayOrder,

            @JsonProperty("response")
            ResponseReviewDTO response,

            @JsonProperty("options")
            List<OptionReviewDTO> options
    ) {
    }

    public record ResponseReviewDTO(

            @JsonProperty("uuid")
            UUID uuid,

            @JsonProperty("attempt_uuid")
            UUID attemptUuid,

            @JsonProperty("question_uuid")
            UUID questionUuid,

            @JsonProperty("selected_option_uuid")
            UUID selectedOptionUuid,

            @JsonProperty("text_response")
            String textResponse,

            @JsonProperty("points_earned")
            BigDecimal pointsEarned,

            @JsonProperty("is_correct")
            Boolean isCorrect
    ) {
    }

    public record OptionReviewDTO(

            @JsonProperty("uuid")
            UUID uuid,

            @JsonProperty("question_uuid")
            UUID questionUuid,

            @JsonProperty("option_text")
            String optionText,

            @JsonProperty("is_correct")
            Boolean isCorrect,

            @JsonProperty("display_order")
            Integer displayOrder
    ) {
    }
}
