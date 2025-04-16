package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UserCertificationDTO(

        @Schema(description = "Date the certificate was issued (ISO 8601 format)", example = "2024-03-10")
        @NotBlank(message = "Issued date is required")
        @JsonProperty("issued_date")
        LocalDate issuedDate,

        @Schema(description = "Name of the issuing organization", example = "Coursera")
        @NotBlank(message = "Issued by is required")
        @Size(max = 100, message = "Issuer name cannot exceed 100 characters")
        @JsonProperty("issued_by")
        String issuedBy,

        @Schema(description = "URL pointing to the certificate resource", example = "https://example.com/certificate/12345")
        @NotBlank(message = "Certificate URL is required")
        @Size(max = 2048, message = "Certificate URL is too long")
        @JsonProperty("certificate_url")
        String certificateUrl,

        @Schema(description = "UUID of the user who owns this certificate", example = "d2e6f6c4-3d44-11ee-be56-0242ac120002")
        @NotBlank(message = "User UUID is required")
        @JsonProperty("user_uuid")
        UUID userUuid

) {
}
