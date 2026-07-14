package apps.sarafrika.elimika.resourcing.dto;

import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "OrganisationResource",
        description = "Bookable resource registered by an organisation: a venue (classroom, lab) or an equipment pool"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganisationResourceDTO(

        @Schema(description = "**[READ-ONLY]** Unique identifier of the resource", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[READ-ONLY]** Organisation owning the resource (taken from the request path)", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "organisation_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID organisationUuid,

        @Schema(description = "Training branch the resource belongs to", nullable = true)
        @JsonProperty("branch_uuid")
        UUID branchUuid,

        @Schema(description = "Resource kind", example = "VENUE", allowableValues = {"VENUE", "EQUIPMENT_POOL"})
        @NotNull(message = "Resource type is required")
        @JsonProperty("resource_type")
        ResourceType resourceType,

        @Schema(description = "Resource name, unique per organisation", example = "Physics Lab B")
        @NotBlank(message = "Resource name is required")
        @JsonProperty("name")
        String name,

        @Schema(description = "Free-form description", nullable = true)
        @JsonProperty("description")
        String description,

        @Schema(description = "Seat capacity (VENUE only)", example = "30", nullable = true)
        @JsonProperty("seat_capacity")
        Integer seatCapacity,

        @Schema(description = "Total available units (EQUIPMENT_POOL only)", example = "25", nullable = true)
        @JsonProperty("total_quantity")
        Integer totalQuantity,

        @Schema(description = "Human readable location", nullable = true)
        @JsonProperty("location_name")
        String locationName,

        @Schema(description = "Latitude of the resource location", nullable = true)
        @JsonProperty("location_latitude")
        BigDecimal locationLatitude,

        @Schema(description = "Longitude of the resource location", nullable = true)
        @JsonProperty("location_longitude")
        BigDecimal locationLongitude,

        @Schema(description = "Whether the resource can currently be booked", example = "true")
        @JsonProperty("is_active")
        Boolean isActive,

        @Schema(description = "**[READ-ONLY]** Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(description = "**[READ-ONLY]** Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate
) {
}
