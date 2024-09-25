package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.config.exception.CourseNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.event.CreateCourseEvent;
import apps.sarafrika.elimika.course.persistence.Course;
import apps.sarafrika.elimika.course.persistence.CourseFactory;
import apps.sarafrika.elimika.course.persistence.CourseRepository;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public ResponseDTO<Void> createCourse(CreateCourseRequestDTO createCourseRequestDTO) {

        final Course course = CourseFactory.create(createCourseRequestDTO);

        eventPublisher.publishEvent(new CreateCourseEvent(course, createCourseRequestDTO));

        courseRepository.save(course);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), COURSE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<CourseResponseDTO> findCourse(Long id) {

        final Course course = findCourseById(id);

        CourseResponseDTO courseResponseDTO = CourseResponseDTO.from(course);

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
                .map(CourseResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(coursesPage.getContent(), coursesPage.getNumber(), coursesPage.getSize(),
                coursesPage.getTotalPages(), coursesPage.getTotalElements(), HttpStatus.OK.value(), COURSES_FOUND_SUCCESS);

    }


    @Transactional
    @Override
    public ResponseDTO<Void> updateCourse(UpdateCourseRequestDTO updateCourseRequestDTO, Long id) {

        final Course course = findCourseById(id);

        CourseFactory.update(course, updateCourseRequestDTO);

        courseRepository.save(course);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), COURSE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }


    @Transactional
    @Override
    public void deleteCourse(Long id) {

        final Course course = findCourseById(id);

        courseRepository.delete(course);
    }
}
