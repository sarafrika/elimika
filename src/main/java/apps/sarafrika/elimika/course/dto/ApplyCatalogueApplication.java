package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** The caller's own training application for a catalogue item. */
@Schema(name = "ApplyCatalogueApplication", description = "The caller's training application for this course or programme.")
public record ApplyCatalogueApplication(
        @JsonProperty("uuid") UUID uuid,
        @Schema(allowableValues = {"pending", "approved", "rejected", "revoked"})
        @JsonProperty("status") String status
) {
}
