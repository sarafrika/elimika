package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCoursePricingRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCoursePricingRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CoursePricingResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;

public interface CoursePricingService {

    ResponseDTO<CoursePricingResponseDTO> createCoursePricing(CreateCoursePricingRequestDTO createCoursePricingRequestDTO, Long courseId);

    ResponseDTO<CoursePricingResponseDTO> findCoursePricingForCourse(Long courseId);

    ResponseDTO<CoursePricingResponseDTO> updateCoursePricing(UpdateCoursePricingRequestDTO updateCoursePricingRequestDTO);

    void deleteCoursePricing(Long coursePricingId);
}
