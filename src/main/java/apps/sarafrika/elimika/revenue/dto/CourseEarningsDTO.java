package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** What one of the caller's courses has earned them, net of platform fees. */
@Schema(name = "CourseEarnings", description = "A course creator's net earnings from one course")
public record CourseEarningsDTO(
        @JsonProperty("course_uuid")
        UUID courseUuid,
        @JsonProperty("sales_count")
        long salesCount,
        @JsonProperty("earned_amount")
        BigDecimal earnedAmount
) {
}
