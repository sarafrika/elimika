package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(name = "CourseEvaluationPlanUpdate", description = "Cells to change; cells not listed are left as they are")
public record CourseEvaluationPlanUpdateRequest(

        @NotNull
        @JsonProperty("cells")
        List<@Valid CourseEvaluationPlanCellRequest> cells
) {
}
