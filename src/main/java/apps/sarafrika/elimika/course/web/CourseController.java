package apps.sarafrika.elimika.course.web;

import apps.sarafrika.elimika.course.dto.request.CourseRequestDTO;
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
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = CourseController.ROOT_PATH)
class CourseController {

    protected static final String ROOT_PATH = "api/v1/courses";
    protected static final String ID_PATH = "{courseId}";

    private final CourseService courseService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<CourseResponseDTO> getCourses(CourseRequestDTO courseRequestDTO, final Pageable pageable) {

        return courseService.findAllCourses(courseRequestDTO, pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<CourseResponseDTO> getCourse(final @PathVariable Long courseId) {

        return courseService.findCourse(courseId);
    }

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<CourseResponseDTO> createCourse(@RequestPart(name = "course") CreateCourseRequestDTO createCourseRequestDTO, @RequestPart(name = "thumbnail") MultipartFile thumbnail) {

        return courseService.createCourse(createCourseRequestDTO, thumbnail);
    }

    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<CourseResponseDTO> updateCourse(@RequestBody UpdateCourseRequestDTO updateCourseRequestDTO, @PathVariable Long courseId) {

        return courseService.updateCourse(updateCourseRequestDTO, courseId);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCourse(final @PathVariable Long courseId) {

        courseService.deleteCourse(courseId);
    }
}

