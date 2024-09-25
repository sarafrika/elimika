package apps.sarafrika.elimika.instructor.web;

import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorAvailabilityResponseDTO;
import apps.sarafrika.elimika.instructor.service.InstructorAvailabilityService;
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
@RequestMapping(path = InstructorAvailabilityController.ROOT_PATH)
class InstructorAvailabilityController {

    protected static final String ROOT_PATH = "api/v1/instructors";

    private final InstructorAvailabilityService instructorAvailabilityService;

    @GetMapping(path = "{instructorId}/availability")
    @ResponseStatus(HttpStatus.OK)
    ResponsePageableDTO<InstructorAvailabilityResponseDTO> getInstructorAvailabilitySlots(final Pageable pageable, @PathVariable Long instructorId) {

        return instructorAvailabilityService.findAllInstructorAvailabilitySlots(instructorId, pageable);
    }

    @GetMapping(path = "{instructorId}/availability/{slotId}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<InstructorAvailabilityResponseDTO> getInstructorAvailabilitySlot(final @PathVariable(name = "slotId") Long id, @PathVariable Long instructorId) {

        return instructorAvailabilityService.findInstructorAvailabilitySlot(instructorId, id);
    }

    @PostMapping(path = "{instructorId}/availability")
    ResponseDTO<Void> createInstructorAvailabilitySlot(@RequestBody CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO, @PathVariable Long instructorId) {

        return instructorAvailabilityService.addInstructorAvailabilitySlot(createInstructorAvailabilityRequestDTO, instructorId);
    }

    @PostMapping(path = "{instructorId}/availability/batch")
    ResponseDTO<Void> createInstructorAvailabilitySlotBatch(@RequestBody Set<CreateInstructorAvailabilityRequestDTO> createInstructorAvailabilityRequestDTOS, @PathVariable Long instructorId) {

        return instructorAvailabilityService.addInstructorAvailabilitySlotBatch(createInstructorAvailabilityRequestDTOS, instructorId);
    }

    @PutMapping(path = "{instructorId}/availability/{slotId}")
    @ResponseStatus(HttpStatus.OK)
    ResponseDTO<Void> updateInstructorAvailabilitySlot(@RequestBody UpdateInstructorAvailabilityRequestDTO updateInstructorAvailabilityRequestDTO,
                                                       @PathVariable(name = "slotId") Long id, @PathVariable Long instructorId) {

        return instructorAvailabilityService.updateInstructorAvailabilitySlot(updateInstructorAvailabilityRequestDTO, instructorId, id);
    }

    @DeleteMapping(path = "{instructorId}/availability/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteInstructorAvailabilitySlot(final @PathVariable(name = "slotId") Long id, @PathVariable Long instructorId) {

        instructorAvailabilityService.deleteInstructorAvailabilitySlot(instructorId, id);
    }
}
