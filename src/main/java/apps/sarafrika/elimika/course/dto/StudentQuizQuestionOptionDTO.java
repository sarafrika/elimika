package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "StudentQuizQuestionOption",
        description = "Student-safe quiz option payload without configured correctness"
)
public record StudentQuizQuestionOptionDTO(

        @JsonProperty("uuid")
        UUID uuid,

        @JsonProperty("question_uuid")
        UUID questionUuid,

        @JsonProperty("option_text")
        String optionText,

        @JsonProperty("display_order")
        Integer displayOrder
) {
}
