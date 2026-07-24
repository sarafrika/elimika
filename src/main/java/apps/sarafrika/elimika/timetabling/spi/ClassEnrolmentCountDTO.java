package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * The active enrolment count for a single class definition owned by an organisation.
 *
 * @param classDefinitionUuid the class definition
 * @param enrolled            number of distinct, actively-enrolled students
 */
@Schema(description = "Active enrolment count for one class definition")
public record ClassEnrolmentCountDTO(
        @Schema(description = "Class definition UUID", format = "uuid")
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,

        @Schema(description = "Distinct actively-enrolled students", example = "18")
        @JsonProperty("enrolled") long enrolled
) {
}
