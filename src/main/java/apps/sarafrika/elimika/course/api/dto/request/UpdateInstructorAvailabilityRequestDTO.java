package apps.sarafrika.elimika.course.api.dto.request;

import java.time.DayOfWeek;
import java.util.Date;

public record UpdateInstructorAvailabilityRequestDTO(
        Date availabilityStart,

        Date availabilityEnd,

        DayOfWeek dayOfWeek,

        Date timeSlotStart,

        Date timeSlotEnd
) {
}
