package apps.sarafrika.elimika.instructor.web;


import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorRequestDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.instructor.service.InstructorService;
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
public class InstructorController {


    protected static final String ROOT_PATH = "api/v1/instructors";
    private static final String ID_PATH = "{id}";

    private final InstructorService instructorService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<InstructorResponseDTO> getInstructors(final Pageable pageable) {

        return instructorService.findAllInstructors(pageable);
    }

    @GetMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<InstructorResponseDTO> getInstructor(final @PathVariable Long id) {

        return instructorService.findInstructor(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseDTO<Void> createInstructor(@RequestBody CreateInstructorRequestDTO createInstructorRequestDTO) {

        return instructorService.createInstructor(createInstructorRequestDTO);
    }

    @PutMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateInstructor(@RequestBody UpdateInstructorRequestDTO updateInstructorRequestDTO, @PathVariable Long id) {

        return instructorService.updateInstructor(updateInstructorRequestDTO, id);
    }

    @DeleteMapping(path = ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteInstructor(final @PathVariable Long id) {

        instructorService.deleteInstructor(id);
    }

}
