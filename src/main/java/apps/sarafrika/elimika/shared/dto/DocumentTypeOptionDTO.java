package apps.sarafrika.elimika.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "DocumentTypeOption", description = "Selectable document type metadata for instructor and course creator uploads")
public record DocumentTypeOptionDTO(
        @Schema(description = "Unique identifier of the document type", example = "35b49d4c-aec0-4a88-873b-5fa91342198f")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Document type code/name", example = "CERTIFICATE")
        @JsonProperty("name")
        String name,

        @Schema(description = "Human-readable description of the document type", example = "Educational certificates and diplomas")
        @JsonProperty("description")
        String description,

        @Schema(description = "Maximum allowed file size for this type in MB", example = "10")
        @JsonProperty("max_file_size_mb")
        Integer maxFileSizeMb,

        @Schema(description = "Allowed file extensions for this document type", example = "[\"pdf\", \"jpg\", \"png\"]")
        @JsonProperty("allowed_extensions")
        List<String> allowedExtensions,

        @Schema(description = "Whether this document type is mandatory in onboarding flows")
        @JsonProperty("is_required")
        Boolean isRequired
) {
}
