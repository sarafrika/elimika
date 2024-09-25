package apps.sarafrika.elimika.instructor.dto.response;


import apps.sarafrika.elimika.instructor.persistence.InstructorAvailability;

import java.time.DayOfWeek;
import java.util.Date;

public record InstructorAvailabilityResponseDTO(
        Long id,

        Date availabilityStart,

        Date availabilityEnd,

        DayOfWeek dayOfWeek,

        Date timeSlotStart,

        Date timeSlotEnd
) {
    public static InstructorAvailabilityResponseDTO from(InstructorAvailability instructorAvailability) {

        return new InstructorAvailabilityResponseDTO(
                instructorAvailability.getId(),
                instructorAvailability.getAvailabilityStart(),
                instructorAvailability.getAvailabilityEnd(),
                instructorAvailability.getDayOfWeek(),
                instructorAvailability.getTimeSlotStart(),
                instructorAvailability.getTimeSlotEnd()
        );
    }
}
