package apps.sarafrika.elimika.course.api;

import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.InstructorAvailabilityResponseDTO;
import apps.sarafrika.elimika.course.api.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping(path = InstructorController.ROOT_PATH)
class InstructorController {

    protected static final String ROOT_PATH = "api/v1/instructors";

    private static final String ID_PATH = "{id}";
    private static final String SLOT_ID = "slot-id";
    private static final String AVAILABILITY_SLOT_ID_PATH = "{" + SLOT_ID + "}";
    private static final String ROOT_AVAILABILITY_SLOT_PATH = "availability-slots";
    private static final String ROOT_BATCH_AVAILABILITY_SLOT_PATH = "availability-slots/batch";

    private final InstructorService instructorService;
    private final InstructorAvailabilityService instructorAvailabilityService;


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


    @GetMapping(path = ID_PATH + "/" + ROOT_AVAILABILITY_SLOT_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<InstructorAvailabilityResponseDTO> getAvailabilitySlots(final Pageable pageable, @PathVariable Long id) {

        return instructorAvailabilityService.findAllByInstructor(pageable, id);
    }


    @GetMapping(path = ID_PATH + "/" + ROOT_AVAILABILITY_SLOT_PATH + "/" + AVAILABILITY_SLOT_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<InstructorAvailabilityResponseDTO> getAvailabilitySlot(final @PathVariable(name = SLOT_ID) Long id) {

        return instructorAvailabilityService.findById(id);
    }


    @PostMapping(path = ID_PATH + "/" + ROOT_AVAILABILITY_SLOT_PATH)
    ResponseDTO<Void> createAvailabilitySlot(@RequestBody CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO, @PathVariable Long id) {

        return instructorAvailabilityService.create(createInstructorAvailabilityRequestDTO, id);
    }


    @PostMapping(path = ID_PATH + "/" + ROOT_BATCH_AVAILABILITY_SLOT_PATH)
    ResponseDTO<Void> createAvailabilitySlotBatch(@RequestBody Set<CreateInstructorAvailabilityRequestDTO> createInstructorAvailabilityRequestDTOS, @PathVariable Long id) {

        return instructorAvailabilityService.createBatch(createInstructorAvailabilityRequestDTOS, id);
    }


    @PutMapping(path = ID_PATH + "/" + ROOT_AVAILABILITY_SLOT_PATH + "/" + AVAILABILITY_SLOT_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateAvailabilitySlot(@RequestBody UpdateInstructorAvailabilityRequestDTO updateInstructorAvailabilityRequestDTO, @PathVariable(name = SLOT_ID) Long id) {

        return instructorAvailabilityService.update(updateInstructorAvailabilityRequestDTO, id);
    }

    @DeleteMapping(path = ID_PATH + "/" + ROOT_AVAILABILITY_SLOT_PATH + "/" + AVAILABILITY_SLOT_ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAvailabilitySlot(final @PathVariable(name = SLOT_ID) Long id) {

        instructorAvailabilityService.delete(id);
    }

}
