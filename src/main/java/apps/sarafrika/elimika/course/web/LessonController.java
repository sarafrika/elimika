package apps.sarafrika.elimika.course.web;

import apps.sarafrika.elimika.course.dto.request.CreateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResponseDTO;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = LessonController.ROOT_PATH)
class LessonController {

    protected static final String ROOT_PATH = "api/v1/courses/{courseId}/lessons";
    private static final String ID_PATH = "{lessonId}";

    private final LessonService lessonService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<LessonResponseDTO> getLessons(final Pageable pageable, final @PathVariable Long courseId) {

        return lessonService.findAllLessons(courseId, pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<LessonResponseDTO> getLesson(final @PathVariable Long courseId, final @PathVariable Long lessonId) {

        return lessonService.findLesson(courseId, lessonId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<LessonResponseDTO> createLesson(final @PathVariable Long courseId, @RequestPart("lesson") CreateLessonRequestDTO createLessonRequestDTO,
                                   @RequestPart("files") List<MultipartFile> files) {

        return lessonService.createLesson(courseId, createLessonRequestDTO, files);
    }

    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateLesson(@RequestBody final UpdateLessonRequestDTO updateLessonRequestDTO, final @PathVariable Long courseId, final @PathVariable Long lessonId) {

        return lessonService.updateLesson(courseId, updateLessonRequestDTO, lessonId);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteLesson(final @PathVariable Long courseId, final @PathVariable Long lessonId) {

        lessonService.deleteLesson(courseId, lessonId);
    }
}

