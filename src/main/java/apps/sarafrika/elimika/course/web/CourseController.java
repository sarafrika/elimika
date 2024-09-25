package apps.sarafrika.elimika.course.web;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
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
class CourseController {

    protected static final String ROOT_PATH = "api/v1/courses";
    protected static final String ID_PATH = "{id}";

    private final CourseService courseService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<CourseResponseDTO> getCourses(final Pageable pageable) {

        return courseService.findAllCourses(pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<CourseResponseDTO> getCourse(final @PathVariable Long id) {

        return courseService.findCourse(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createCourse(@RequestBody CreateCourseRequestDTO createCourseRequestDTO) {

        return courseService.createCourse(createCourseRequestDTO);
    }

    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateCourse(@RequestBody UpdateCourseRequestDTO updateCourseRequestDTO, @PathVariable Long id) {

        return courseService.updateCourse(updateCourseRequestDTO, id);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCourse(final @PathVariable Long id) {

        courseService.deleteCourse(id);
    }
}

