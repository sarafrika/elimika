package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.model.CourseEnrollmentFeedback;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "CourseEnrollmentFeedback", description = "An instructor's comment on a learner's progress; the newest is current")
public record CourseEnrollmentFeedbackDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "enrollment_uuid", access = JsonProperty.Access.READ_ONLY) UUID enrollmentUuid,
        @JsonProperty("comments") @NotBlank @Size(max = 20000) String comments,
        @JsonProperty(value = "author_user_uuid", access = JsonProperty.Access.READ_ONLY) UUID authorUserUuid,
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime createdDate
) {

    public static CourseEnrollmentFeedbackDTO from(CourseEnrollmentFeedback feedback) {
        return new CourseEnrollmentFeedbackDTO(feedback.getUuid(), feedback.getEnrollmentUuid(), feedback.getComments(),
                feedback.getAuthorUserUuid(), feedback.getCreatedDate());
    }
}
