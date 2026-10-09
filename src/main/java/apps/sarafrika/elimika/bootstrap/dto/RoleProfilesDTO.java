package apps.sarafrika.elimika.bootstrap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** The caller's role profile identifiers; each is null when that profile does not exist. */
@Schema(name = "BootstrapRoleProfiles", description = "Role profile UUIDs owned by the caller")
public record RoleProfilesDTO(
        @JsonProperty("student_uuid")
        UUID studentUuid,
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,
        @JsonProperty("course_creator_uuid")
        UUID courseCreatorUuid
) {
}
