package apps.sarafrika.elimika.coursecreator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record CourseCreatorModerationRequest(
        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        @JsonProperty("reason")
        String reason
) {
}
