package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(
        name = "AssignmentSubmissionRequest",
        description = "Student assignment submission request"
)
public record AssignmentSubmissionRequest(

        @Schema(
                description = "Course enrollment UUID for the student submitting the assignment.",
                example = "e1n2r3o4-5l6l-7m8e-9n10-abcdefghijkl"
        )
        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @Schema(
                description = "Student UUID. Used to resolve the active course enrollment when enrollment_uuid is omitted.",
                example = "s1t2u3d4-5e6n-7t8u-9u10-abcdefghijkl"
        )
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(
                description = "Text content of the student's submission.",
                example = "My assignment response.",
                maxLength = 10000
        )
        @Size(max = 10000, message = "Submission text must not exceed 10000 characters")
        @JsonProperty("submission_text")
        String submissionText,

        @Schema(
                description = "External or previously uploaded file URLs attached to this submission.",
                example = "[\"https://storage.sarafrika.com/submissions/work.pdf\"]"
        )
        @JsonProperty("file_urls")
        String[] fileUrls
) {
}
