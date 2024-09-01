package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = CourseController.ROOT_PATH)
public class CourseController {

    protected static final String ROOT_PATH = "api/v1/courses";
    protected static final String ID_PATH = "{id}";

    private final CourseService courseService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponsePageableDTO<CourseResponseDTO> getCourses(final Pageable pageable) {

        return courseService.findAll(pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public ResponseDTO<CourseResponseDTO> getCourse(final @PathVariable Long id) {

        return courseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDTO<Void> createCourse(@RequestBody CreateCourseRequestDTO createCourseRequestDTO) {

        return courseService.create(createCourseRequestDTO);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(final @PathVariable Long id) {

        courseService.delete(id);
    }
}
