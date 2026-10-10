package apps.sarafrika.elimika.resourcing.dto;

import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(
        name = "OrganisationResourceCalendar",
        description = "One active resource of an organisation with its merged calendar entries for the requested range"
)
public record OrganisationResourceCalendarDTO(

        @Schema(description = "Resource the entries belong to")
        @JsonProperty("resource_uuid")
        UUID resourceUuid,

        @Schema(description = "Resource name", example = "Physics Lab B")
        @JsonProperty("resource_name")
        String resourceName,

        @Schema(description = "Resource kind", example = "VENUE", allowableValues = {"VENUE", "EQUIPMENT_POOL"})
        @JsonProperty("resource_type")
        ResourceType resourceType,

        @Schema(description = "Merged calendar entries, sorted by start time")
        @JsonProperty("entries")
        List<ResourceCalendarEntryDTO> entries
) {
}
