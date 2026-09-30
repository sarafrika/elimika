package apps.sarafrika.elimika.skills.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "Skill", description = "An entry of the admin-curated skills taxonomy")
public record SkillDTO(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("name") String name,
        @Schema(description = "Unique, lower-case, hyphenated; derived from the name unless set", example = "java-programming")
        @JsonProperty("slug") String slug,
        @Schema(description = "Broader skill this one sits under, if any")
        @JsonProperty("parent_uuid") UUID parentUuid,
        @Schema(description = "Alternative names; they resolve to this skill and act as search synonyms")
        @JsonProperty("aliases") List<String> aliases,
        @Schema(description = "False once retired: existing tags keep it, it can no longer be picked")
        @JsonProperty("active") boolean active,
        @JsonProperty("created_date") LocalDateTime createdDate,
        @JsonProperty("updated_date") LocalDateTime updatedDate
) {
}
