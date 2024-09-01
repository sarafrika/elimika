package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.exceptions.custom.ResourceNotFoundException;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final String ERROR_COURSE_NOT_FOUND = "Course not found";
    private static final String COURSE_CREATED_SUCCESS = "Course has been persisted successfully";
    private static final String COURSES_FOUND_SUCCESS = "Courses retrieved successfully";
    private static final String COURSE_FOUND_SUCCESS = "Course retrieved successfully";

    private final CourseRepository courseRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseDTO<CourseResponseDTO> findById(Long id) {

        CourseResponseDTO courseResponseDTO = CourseResponseDTO.from(courseRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(ERROR_COURSE_NOT_FOUND)));

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

        courseRepository.save(course);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), COURSE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public CourseResponseDTO update(Course course) {
        return null;
    }

    @Override
    public void delete(int id) {

    }
}
