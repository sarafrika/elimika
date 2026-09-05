package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * The commercial picture of a course, for the person whose course it is.
 * <p>
 * Present only for the course creator and platform admins. An approved trainer sees their own
 * takings in the scoped block and never these totals: what a course earns across every school
 * delivering it is the creator's business, not a competitor's.
 */
@Schema(name = "CourseStatsOwner", description = "Commercial totals for the course. Absent unless the caller is the creator or a platform admin.")
public record CourseStatsOwnerDTO(

        @Schema(description = "Every enrolment the course has taken, at any status.", example = "530")
        @JsonProperty("total_enrollments")
        long totalEnrollments,

        @Schema(description = "Captured line totals for the course.", example = "1420000.00")
        @JsonProperty("gross_sales")
        BigDecimal grossSales,

        @Schema(description = "The platform's share of those captured lines.", example = "142000.00")
        @JsonProperty("platform_fee")
        BigDecimal platformFee,

        @Schema(description = "Distinct captured orders containing the course.", example = "318")
        @JsonProperty("paid_orders")
        long paidOrders,

        @Schema(description = "Distinct orders containing the course that were refunded, wholly or partly.", example = "4")
        @JsonProperty("refunded_orders")
        long refundedOrders
) {
}
