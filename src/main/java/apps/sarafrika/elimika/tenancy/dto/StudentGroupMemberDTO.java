package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "StudentGroupMember", description = "A student's membership within a group.")
public record StudentGroupMemberDTO(

        @Schema(description = "Unique identifier.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "Group UUID.")
        @JsonProperty("group_uuid")
        UUID groupUuid,

        @Schema(description = "Student user UUID.")
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(description = "When the student was added.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
