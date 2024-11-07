package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.CoursePricing;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CoursePricingResponseDTO(
        Long id,

        BigDecimal basePrice,

        BigDecimal discountRate,

        Date discountStart,

        Date discountEnd,

        String discountCode,

        BigDecimal finalPrice,

        boolean free
) {

    public static CoursePricingResponseDTO from(CoursePricing coursePricing) {

        return new CoursePricingResponseDTO(
                coursePricing.getId(),
                coursePricing.getBasePrice(),
                coursePricing.getDiscountRate(),
                coursePricing.getDiscountStart(),
                coursePricing.getDiscountEnd(),
                coursePricing.getDiscountCode(),
                coursePricing.getFinalPrice(),
                coursePricing.isFree()
        );
    }
}
