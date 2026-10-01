package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * The classes of a public course a visitor can still join, and the lowest fee among them.
 */
@Schema(name = "CourseOpenClasses", description = "The joinable classes of a public course and the cheapest fee.")
public record CourseOpenClasses(

        @Schema(description = "The lowest class fee among the listed classes. Null when there are no classes "
                + "or none has a fee. This, not the course's own price, is what a learner pays.",
                example = "2500.00")
        @JsonProperty("price_from")
        BigDecimal priceFrom,

        @Schema(description = "ISO 4217 currency of price_from. Null when there are no classes.", example = "KES")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Number of classes listed.", example = "3")
        @JsonProperty("open_class_count")
        int openClassCount,

        @Schema(description = "The classes, cheapest first, then soonest start.")
        @JsonProperty("classes")
        List<OpenClassSummary> classes
) {
}
