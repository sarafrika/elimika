package apps.sarafrika.elimika.skills.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(name = "SkillRequest", description = "Creates or replaces a skills taxonomy entry")
public record SkillRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must not exceed 255 characters")
        @JsonProperty("name") String name,

        @Schema(description = "Optional; derived from the name when omitted. Normalised to lower-case words joined by hyphens")
        @Size(max = 255, message = "slug must not exceed 255 characters")
        @JsonProperty("slug") String slug,

        @Schema(description = "Optional broader skill; may not be the skill itself or one of its descendants")
        @JsonProperty("parent_uuid") UUID parentUuid,

        @Size(max = 50, message = "at most 50 aliases")
        @JsonProperty("aliases") List<@Size(max = 255) String> aliases,

        @Schema(description = "Defaults to true")
        @JsonProperty("active") Boolean active
) {
}
