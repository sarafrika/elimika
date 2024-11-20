package apps.sarafrika.elimika.instructor.dto.request;

import java.time.DayOfWeek;
import java.util.Date;

public record CreateInstructorAvailabilityRequestDTO(
        Date availabilityStart,

        Date availabilityEnd,

        DayOfWeek dayOfWeek,

        Date timeSlotStart,

        Date timeSlotEnd
) {
}
