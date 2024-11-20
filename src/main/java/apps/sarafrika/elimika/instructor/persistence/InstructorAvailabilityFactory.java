package apps.sarafrika.elimika.instructor.persistence;


import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorAvailabilityRequestDTO;

public class InstructorAvailabilityFactory {

    public static InstructorAvailability create(CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO) {

        return InstructorAvailability.builder()
                .availabilityStart(createInstructorAvailabilityRequestDTO.availabilityStart())
                .availabilityEnd(createInstructorAvailabilityRequestDTO.availabilityEnd())
                .dayOfWeek(createInstructorAvailabilityRequestDTO.dayOfWeek())
                .timeSlotStart(createInstructorAvailabilityRequestDTO.timeSlotStart())
                .timeSlotEnd(createInstructorAvailabilityRequestDTO.timeSlotEnd())
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
