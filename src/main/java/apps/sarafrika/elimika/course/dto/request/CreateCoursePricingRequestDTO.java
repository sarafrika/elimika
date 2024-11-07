package apps.sarafrika.elimika.course.dto.request;

import java.math.BigDecimal;
import java.util.Date;

public record CreateCoursePricingRequestDTO(
        Long courseId,

        BigDecimal basePrice,

        BigDecimal discountRate,

        Date discountStart,

        Date discountEnd,

        String discountCode,

        boolean free
) {
}
