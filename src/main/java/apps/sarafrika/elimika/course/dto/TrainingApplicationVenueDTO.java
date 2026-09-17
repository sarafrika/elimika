package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** A venue offered with a training application, resolved from the organisation's resources. */
@Schema(name = "TrainingApplicationVenue", description = "A venue the applicant organisation offers for delivering the training")
public record TrainingApplicationVenueDTO(

        @Schema(description = "**[READ-ONLY]** The venue resource.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "resource_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID resourceUuid,

        @Schema(description = "**[READ-ONLY]** Venue name; null if the resource no longer exists.", nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "name", access = JsonProperty.Access.READ_ONLY)
        String name,

        @Schema(description = "**[READ-ONLY]** Seats in the venue.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "seat_capacity", access = JsonProperty.Access.READ_ONLY)
        Integer seatCapacity,

        @Schema(description = "**[READ-ONLY]** Free-text location of the venue.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "location_name", access = JsonProperty.Access.READ_ONLY)
        String locationName,

        @Schema(description = "**[READ-ONLY]** Branch the venue belongs to.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "branch_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID branchUuid,

        @Schema(description = "**[READ-ONLY]** Name of that branch.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "branch_name", access = JsonProperty.Access.READ_ONLY)
        String branchName
) {
}
