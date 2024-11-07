package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.CoursePricingNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateCoursePricingRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCoursePricingRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CoursePricingResponseDTO;
import apps.sarafrika.elimika.course.persistence.CoursePricing;
import apps.sarafrika.elimika.course.persistence.CoursePricingFactory;
import apps.sarafrika.elimika.course.persistence.CoursePricingRepository;
import apps.sarafrika.elimika.course.service.CoursePricingService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
class CoursePricingServiceImpl implements CoursePricingService {

    private static final String COURSE_PRICING_CREATED_SUCCESS = "Course pricing has been persisted successfully.";
    private static final String ERROR_COURSE_PRICING_NOT_FOUND = "Course pricing not found.";

    private final CoursePricingRepository coursePricingRepository;

    @Override
    public ResponseDTO<CoursePricingResponseDTO> createCoursePricing(CreateCoursePricingRequestDTO createCoursePricingRequestDTO, Long courseId) {

        CoursePricing coursePricing = CoursePricingFactory.create(createCoursePricingRequestDTO);

        coursePricing.setCourseId(courseId);

        coursePricingRepository.save(coursePricing);

        return new ResponseDTO<>(CoursePricingResponseDTO.from(coursePricing), HttpStatus.CREATED.value(), COURSE_PRICING_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<CoursePricingResponseDTO> findCoursePricingForCourse(Long courseId) {
        log.info("Finding course pricing for course with id: {}", courseId);

        CoursePricing coursePricing = findCoursePricingByCourseId(courseId);

        return new ResponseDTO<>(CoursePricingResponseDTO.from(coursePricing), HttpStatus.OK.value(), null, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<CoursePricingResponseDTO> updateCoursePricing(UpdateCoursePricingRequestDTO updateCoursePricingRequestDTO) {

        CoursePricing coursePricing = findCoursePricingById(updateCoursePricingRequestDTO.id());

        CoursePricingFactory.update(coursePricing, updateCoursePricingRequestDTO);

        coursePricingRepository.save(coursePricing);

        return new ResponseDTO<>(CoursePricingResponseDTO.from(coursePricing), HttpStatus.OK.value(), null, null, LocalDateTime.now());
    }

    @Override
    public void deleteCoursePricing(Long coursePricingId) {

        CoursePricing coursePricing = findCoursePricingById(coursePricingId);

        coursePricingRepository.delete(coursePricing);
    }

    private CoursePricing findCoursePricingById(Long id) {

        return coursePricingRepository.findById(id).orElseThrow(() -> new CoursePricingNotFoundException(ERROR_COURSE_PRICING_NOT_FOUND));
    }

    private CoursePricing findCoursePricingByCourseId(Long courseId) {

        return coursePricingRepository.findByCourseId(courseId).orElseThrow(() -> new CoursePricingNotFoundException(ERROR_COURSE_PRICING_NOT_FOUND));
    }
}
