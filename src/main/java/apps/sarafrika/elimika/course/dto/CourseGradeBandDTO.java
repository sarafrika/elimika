package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.model.CourseGradeBand;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "CourseGradeBand", description = "A label awarded once the final grade reaches min_percentage, e.g. Merit from 80%")
public record CourseGradeBandDTO(
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY) UUID uuid,
        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY) UUID courseUuid,
        @JsonProperty("name") @NotBlank @Size(max = 50) String name,
        @JsonProperty("min_percentage") @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal minPercentage,
        @JsonProperty("display_order") Integer displayOrder
) {

    public static CourseGradeBandDTO from(CourseGradeBand band) {
        return new CourseGradeBandDTO(band.getUuid(), band.getCourseUuid(), band.getName(), band.getMinPercentage(),
                band.getDisplayOrder());
    }
}
