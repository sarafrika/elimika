package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** A saved age group with the instructor or organisation that keeps it. */
@Schema(name = "SavedAgeGroup", description = "A reusable age group kept by an instructor or organisation")
public record SavedAgeGroupDTO(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("name") String name,
        @JsonProperty("min_age") Integer minAge,
        @JsonProperty("max_age") Integer maxAge,
        @Schema(description = "instructor or organisation", allowableValues = {"instructor", "organisation"})
        @JsonProperty("owner_type") AgeGroupOwnerType ownerType,
        @JsonProperty("owner_uuid") UUID ownerUuid,
        @Schema(description = "The organisation's name for an organisation's group; null for the caller's own.", nullable = true)
        @JsonProperty("owner_name") String ownerName
) {
}
