package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.QuestionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "StudentQuizQuestion",
        description = "Student-safe quiz question payload"
)
public record StudentQuizQuestionDTO(

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

        @JsonProperty("options")
        List<StudentQuizQuestionOptionDTO> options
) {
}
