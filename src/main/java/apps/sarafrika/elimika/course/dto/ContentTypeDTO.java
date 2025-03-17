package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ContentTypeDTO(

        @JsonProperty("uuid")
        UUID uuid,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @JsonProperty("name")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        @JsonProperty("description")
        String description,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String lastModifiedBy,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime lastModifiedDate

) {
}
