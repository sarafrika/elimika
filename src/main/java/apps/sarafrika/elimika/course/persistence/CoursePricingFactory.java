package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreateCoursePricingRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCoursePricingRequestDTO;

public class CoursePricingFactory {

    public static CoursePricing create(CreateCoursePricingRequestDTO createCoursePricingRequestDTO) {

        return CoursePricing.builder()
                .basePrice(createCoursePricingRequestDTO.basePrice())
                .discountRate(createCoursePricingRequestDTO.discountRate())
                .discountStart(createCoursePricingRequestDTO.discountStart())
                .discountEnd(createCoursePricingRequestDTO.discountEnd())
                .discountCode(createCoursePricingRequestDTO.discountCode())
                .free(createCoursePricingRequestDTO.free())
                .courseId(createCoursePricingRequestDTO.courseId())
                .build();
    }

    public static void update(CoursePricing coursePricing, UpdateCoursePricingRequestDTO updateCoursePricingRequestDTO) {

        coursePricing.setBasePrice(updateCoursePricingRequestDTO.basePrice());
        coursePricing.setDiscountRate(updateCoursePricingRequestDTO.discountRate());
        coursePricing.setDiscountStart(updateCoursePricingRequestDTO.discountStart());
        coursePricing.setDiscountEnd(updateCoursePricingRequestDTO.discountEnd());
        coursePricing.setDiscountCode(updateCoursePricingRequestDTO.discountCode());
        coursePricing.setFree(updateCoursePricingRequestDTO.free());
    }
}
