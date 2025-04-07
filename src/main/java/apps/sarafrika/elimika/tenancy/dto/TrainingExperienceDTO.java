package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record TrainingExperienceDTO(

        @Schema(description = "Name of the organisation where the experience was gained", example = "World Health Organization")
        @NotBlank(message = "Organisation name is required")
        @Size(max = 100, message = "Organisation name cannot exceed 100 characters")
        @JsonProperty("organisation_name")
        String organisationName,

        @Schema(description = "Job title held during the training or experience", example = "Health Program Analyst")
        @NotBlank(message = "Job title is required")
        @Size(max = 100, message = "Job title cannot exceed 100 characters")
        @JsonProperty("job_title")
        String jobTitle,

        @Schema(description = "Description of responsibilities and tasks performed", example = "Managed regional health analytics dashboard and provided training to local analysts.")
        @NotBlank(message = "Work description is required")
        @Size(max = 1000, message = "Work description is too long")
        @JsonProperty("work_description")
        String workDescription,

        @Schema(description = "Start date of the training/experience (ISO 8601 format)", example = "2021-01-10")
        @NotBlank(message = "Start date is required")
        @JsonProperty("start_date")
        LocalDate startDate,

        @Schema(description = "End date of the training/experience (ISO 8601 format)", example = "2022-12-20")
        @NotBlank(message = "End date is required")
        @JsonProperty("end_date")
        LocalDate endDate,

        @Schema(description = "UUID of the user associated with this training/experience", example = "ca4f98b1-11d1-4a74-b4fa-120b5cbeed2b")
        @NotBlank(message = "User UUID is required")
        @JsonProperty("user_uuid")
        UUID userUuid

) {
}
