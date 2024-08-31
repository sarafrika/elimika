package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = CourseController.PATH)
public class CourseController {

    protected static final String PATH = "api/v1/courses";

    private final CourseService courseService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponsePageableDTO<CourseResponseDTO> getCourses(final Pageable pageable) {

        return courseService.findAll(pageable);
    }
}
