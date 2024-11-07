package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.CourseNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CoursePricingResponseDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.event.CreateCourseEvent;
import apps.sarafrika.elimika.course.persistence.Course;
import apps.sarafrika.elimika.course.persistence.CourseFactory;
import apps.sarafrika.elimika.course.persistence.CourseRepository;
import apps.sarafrika.elimika.course.service.CoursePricingService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class CourseServiceImpl implements CourseService {

    private static final String ERROR_COURSE_NOT_FOUND = "Course not found.";
    private static final String COURSE_FOUND_SUCCESS = "Course retrieved successfully.";
    private static final String COURSE_CREATED_SUCCESS = "Course has been persisted successfully.";
    private static final String COURSES_FOUND_SUCCESS = "Courses retrieved successfully.";
    private static final String COURSE_UPDATED_SUCCESS = "Course has been updated successfully.";

    private final CourseRepository courseRepository;
    private final CoursePricingService coursePricingService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public ResponseDTO<CourseResponseDTO> createCourse(CreateCourseRequestDTO createCourseRequestDTO) {

        final Course course = CourseFactory.create(createCourseRequestDTO);

        eventPublisher.publishEvent(new CreateCourseEvent(course, createCourseRequestDTO));

        Course savedCourse = courseRepository.save(course);

        ResponseDTO<CoursePricingResponseDTO> pricing = coursePricingService.createCoursePricing(createCourseRequestDTO.pricing(), course.getId());

        return new ResponseDTO<>(CourseResponseDTO.from(savedCourse, pricing.data()), HttpStatus.CREATED.value(), COURSE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<CourseResponseDTO> findCourse(Long courseId) {

        final Course course = findCourseById(courseId);

        ResponseDTO<CoursePricingResponseDTO> pricing = coursePricingService.findCoursePricingForCourse(courseId);

        CourseResponseDTO courseResponseDTO = CourseResponseDTO.from(course, pricing.data());

        return new ResponseDTO<>(courseResponseDTO, HttpStatus.OK.value(), COURSE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private Course findCourseById(Long id) {

        return courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(ERROR_COURSE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<CourseResponseDTO> findAllCourses(Pageable pageable) {

        Page<CourseResponseDTO> coursesPage = courseRepository.findAll(pageable)
                .stream()
                .map(course -> CourseResponseDTO.from(course, coursePricingService.findCoursePricingForCourse(course.getId()).data()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(coursesPage.getContent(), coursesPage.getNumber(), coursesPage.getSize(),
                coursesPage.getTotalPages(), coursesPage.getTotalElements(), HttpStatus.OK.value(), COURSES_FOUND_SUCCESS);

    }


    @Transactional
    @Override
    public ResponseDTO<CourseResponseDTO> updateCourse(UpdateCourseRequestDTO updateCourseRequestDTO, Long courseId) {

        final Course course = findCourseById(courseId);

        CourseFactory.update(course, updateCourseRequestDTO);

        ResponseDTO<CoursePricingResponseDTO> pricing = coursePricingService.updateCoursePricing(updateCourseRequestDTO.pricing());

        courseRepository.save(course);

        return new ResponseDTO<>(CourseResponseDTO.from(course, pricing.data()), HttpStatus.OK.value(), COURSE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }


    @Transactional
    @Override
    public void deleteCourse(Long courseId) {

        final Course course = findCourseById(courseId);

        courseRepository.delete(course);
    }
}
