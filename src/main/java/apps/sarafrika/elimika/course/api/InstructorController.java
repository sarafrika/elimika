package apps.sarafrika.elimika.course.api;

import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = InstructorController.ROOT_PATH)
class InstructorController {

    protected static final String ROOT_PATH = "api/v1/instructors";
    protected static final String ID_PATH = "{id}";

    private final InstructorService instructorService;


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<InstructorResponseDTO> getInstructors(final Pageable pageable) {

        return instructorService.findAll(pageable);
    }


    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<InstructorResponseDTO> getInstructor(final @PathVariable Long id) {

        return instructorService.findById(id);
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createInstructor(@RequestBody CreateInstructorRequestDTO createInstructorRequestDTO) {

        return instructorService.create(createInstructorRequestDTO);
    }


    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateInstructor(@RequestBody UpdateInstructorRequestDTO updateInstructorRequestDTO, @PathVariable Long id) {

        return instructorService.update(updateInstructorRequestDTO, id);
    }


    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteInstructor(final @PathVariable Long id) {

        instructorService.delete(id);
    }
}
