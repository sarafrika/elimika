package apps.sarafrika.elimika.course.dto.request;

import java.math.BigDecimal;

public record PricingRequestDTO(
        boolean isFree,

        BigDecimal originalPrice,

        BigDecimal salePrice
) {
}
