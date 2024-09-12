package apps.sarafrika.elimika.course.domain;

import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorAvailabilityRequestDTO;

public class InstructorAvailabilityFactory {
    public static InstructorAvailability create(CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO, final Instructor instructor) {

        return InstructorAvailability.builder()
                .availabilityStart(createInstructorAvailabilityRequestDTO.availabilityStart())
                .availabilityEnd(createInstructorAvailabilityRequestDTO.availabilityEnd())
                .dayOfWeek(createInstructorAvailabilityRequestDTO.dayOfWeek())
                .timeSlotStart(createInstructorAvailabilityRequestDTO.timeSlotStart())
                .timeSlotEnd(createInstructorAvailabilityRequestDTO.timeSlotEnd())
                .instructor(instructor)
                .build();
    }

    public static void update(UpdateInstructorAvailabilityRequestDTO updateInstructorAvailabilityRequestDTO, final InstructorAvailability instructorAvailability) {

        instructorAvailability.setAvailabilityStart(updateInstructorAvailabilityRequestDTO.availabilityStart());
        instructorAvailability.setAvailabilityEnd(updateInstructorAvailabilityRequestDTO.availabilityEnd());
        instructorAvailability.setDayOfWeek(updateInstructorAvailabilityRequestDTO.dayOfWeek());
        instructorAvailability.setTimeSlotStart(updateInstructorAvailabilityRequestDTO.timeSlotStart());
        instructorAvailability.setTimeSlotEnd(updateInstructorAvailabilityRequestDTO.timeSlotEnd());
    }
}
