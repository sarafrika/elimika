package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.Course;

import java.math.BigDecimal;

public record PricingResponseDTO(
        boolean isFree,

        BigDecimal originalPrice,

        BigDecimal salePrice
) {
    public static PricingResponseDTO from(Course course) {

        return new PricingResponseDTO(course.isFree(), course.getOriginalPrice(), course.getSalePrice());
    }
}
