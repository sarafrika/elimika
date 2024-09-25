package apps.sarafrika.elimika.course.web;

import apps.sarafrika.elimika.course.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateClassRequestDTO;
import apps.sarafrika.elimika.course.dto.response.ClassResponseDTO;
import apps.sarafrika.elimika.course.service.ClassService;
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

        return classService.findAllClasses(pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<ClassResponseDTO> getClass(final @PathVariable Long id) {

        return classService.findClass(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createClass(@RequestBody CreateClassRequestDTO createClassRequestDTO) {

        return classService.createClass(createClassRequestDTO);
    }

    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateClass(@RequestBody UpdateClassRequestDTO updateClassRequestDTO, @PathVariable final Long id) {

        return classService.updateClass(updateClassRequestDTO, id);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteClass(final @PathVariable Long id) {

        classService.deleteClass(id);
    }

}
