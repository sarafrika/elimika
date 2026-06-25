package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.QuizScope;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "StudentQuiz",
        description = "Student-safe quiz payload without configured answer keys"
)
public record StudentQuizDTO(

        @JsonProperty("uuid")
        UUID uuid,

        @JsonProperty("lesson_uuid")
        UUID lessonUuid,

        @JsonProperty("scope")
        QuizScope scope,

        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @JsonProperty("title")
        String title,

        @JsonProperty("description")
        String description,

        @JsonProperty("instructions")
        String instructions,

        @JsonProperty("time_limit_minutes")
        Integer timeLimitMinutes,

        @JsonProperty("attempts_allowed")
        Integer attemptsAllowed,

        @JsonProperty("passing_score")
        BigDecimal passingScore,

        @JsonProperty("questions")
        List<StudentQuizQuestionDTO> questions
) {
}
