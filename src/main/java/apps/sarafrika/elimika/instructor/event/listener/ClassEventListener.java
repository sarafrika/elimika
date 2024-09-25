package apps.sarafrika.elimika.instructor.event.listener;

import apps.sarafrika.elimika.course.event.CreateClassEvent;
import apps.sarafrika.elimika.course.event.UpdateClassEvent;
import apps.sarafrika.elimika.instructor.dto.response.InstructorAvailabilityResponseDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.instructor.service.InstructorAvailabilityService;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassEventListener {

    private final InstructorService instructorService;
    private final InstructorAvailabilityService instructorAvailabilityService;

    @EventListener
    void onClassCreationEvent(CreateClassEvent event) {

        assignInstructorAndInstructorAvailabilitySlot(event);
    }

    private void assignInstructorAndInstructorAvailabilitySlot(CreateClassEvent event) {

        final ResponseDTO<InstructorResponseDTO> instructor = instructorService.findInstructor(event.createClassRequestDTO().instructorId());

        final ResponseDTO<InstructorAvailabilityResponseDTO> instructorAvailabilitySlot = instructorAvailabilityService.findInstructorAvailabilitySlot(instructor.data().id(), event.createClassRequestDTO().instructorAvailabilityId());

        event.classEntity().setInstructorId(instructor.data().id());
        event.classEntity().setInstructorAvailabilityId(instructorAvailabilitySlot.data().id());
    }

    @EventListener
    void onClassUpdateEvent(UpdateClassEvent event) {

        assignInstructorAndInstructorAvailabilitySlot(event);
    }

    private void assignInstructorAndInstructorAvailabilitySlot(UpdateClassEvent event) {

        final ResponseDTO<InstructorResponseDTO> instructor = instructorService.findInstructor(event.updateClassRequestDTO().instructorId());

        final ResponseDTO<InstructorAvailabilityResponseDTO> instructorAvailabilitySlot = instructorAvailabilityService.findInstructorAvailabilitySlot(instructor.data().id(), event.updateClassRequestDTO().instructorAvailabilityId());

        event.classEntity().setInstructorId(instructor.data().id());
        event.classEntity().setInstructorAvailabilityId(instructorAvailabilitySlot.data().id());
    }

}
