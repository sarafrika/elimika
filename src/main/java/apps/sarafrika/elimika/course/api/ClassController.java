package apps.sarafrika.elimika.course.api;

import apps.sarafrika.elimika.course.api.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateClassRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.ClassResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = ClassController.ROOT_PATH)
class ClassController {

    protected static final String ROOT_PATH = "api/v1/classes";
    protected static final String ID_PATH = "{id}";

    private final ClassService classService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<ClassResponseDTO> getClasses(final Pageable pageable) {

        return classService.findAll(pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<ClassResponseDTO> getClass(final @PathVariable Long id) {

        return classService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createClass(@RequestBody CreateClassRequestDTO createClassRequestDTO) {

        return classService.create(createClassRequestDTO);
    }

    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateClass(@RequestBody UpdateClassRequestDTO updateClassRequestDTO, @PathVariable final Long id) {

        return classService.update(updateClassRequestDTO, id);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteClass(final @PathVariable Long id) {

        classService.delete(id);
    }
}
