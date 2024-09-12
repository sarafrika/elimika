package apps.sarafrika.elimika.course.application;

import apps.sarafrika.elimika.course.api.CourseService;
import apps.sarafrika.elimika.course.api.InstructorService;
import apps.sarafrika.elimika.course.api.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.application.exceptions.CourseNotFoundException;
import apps.sarafrika.elimika.course.application.exceptions.InstructorNotFoundException;
import apps.sarafrika.elimika.course.domain.Course;
import apps.sarafrika.elimika.course.domain.CourseFactory;
import apps.sarafrika.elimika.course.domain.CourseRepository;
import apps.sarafrika.elimika.course.domain.Instructor;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final String ERROR_COURSE_NOT_FOUND = "Course not found.";
    private static final String ERROR_INSTRUCTOR_NOT_FOUND = "Instructor not found.";
    private static final String COURSE_FOUND_SUCCESS = "Course retrieved successfully.";
    private static final String COURSES_FOUND_SUCCESS = "Courses retrieved successfully.";
    private static final String COURSE_UPDATED_SUCCESS = "Course has been updated successfully.";
    private static final String COURSE_CREATED_SUCCESS = "Course has been persisted successfully.";

    private final CourseRepository courseRepository;
    private final InstructorService instructorService;

    @Override
    @Transactional(readOnly = true)
    public ResponseDTO<CourseResponseDTO> findById(Long id) {

        final Course course = findCourseById(id);

        CourseResponseDTO courseResponseDTO = CourseResponseDTO.from(course);

        return new ResponseDTO<>(courseResponseDTO, HttpStatus.OK.value(), COURSE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public ResponsePageableDTO<CourseResponseDTO> findAll(Pageable pageable) {

        Page<CourseResponseDTO> coursesPage = courseRepository.findAll(pageable)
                .stream()
                .map(CourseResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(coursesPage.getContent(), coursesPage.getNumber(), coursesPage.getSize(),
                coursesPage.getTotalPages(), coursesPage.getTotalElements(), HttpStatus.OK.value(), COURSES_FOUND_SUCCESS);
    }

    @Override
    @Transactional
    public ResponseDTO<Void> create(CreateCourseRequestDTO createCourseRequestDTO) {

        final Course course = CourseFactory.create(createCourseRequestDTO);

        addInstructors(createCourseRequestDTO, course);

        courseRepository.save(course);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), COURSE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    @Transactional
    public ResponseDTO<Void> update(UpdateCourseRequestDTO updateCourseRequestDTO, Long id) {

        final Course courseToUpdate = findCourseById(id);

        CourseFactory.update(courseToUpdate, updateCourseRequestDTO);
        courseRepository.save(courseToUpdate);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), COURSE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void delete(Long id) {

        final Course course = findCourseById(id);

        courseRepository.delete(course);
    }

    private Course findCourseById(Long id) {

        return courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(ERROR_COURSE_NOT_FOUND));
    }

    private void addInstructors(final CreateCourseRequestDTO createCourseRequestDTO, final Course course) {

        final Set<Long> requestInstructorIds = createCourseRequestDTO.instructors();

        final Set<Instructor> foundInstructors = instructorService.findByIds(requestInstructorIds);

        final Set<Long> missingInstructorIds = requestInstructorIds.stream()
                .filter(requestInstructorId -> foundInstructors.stream().noneMatch(foundInstructor -> foundInstructor.getId().equals(requestInstructorId)))
                .collect(Collectors.toSet());

        if (!missingInstructorIds.isEmpty()) {

            throw new InstructorNotFoundException(ERROR_INSTRUCTOR_NOT_FOUND);
        }

        course.setInstructors(foundInstructors);
    }
}
