package apps.sarafrika.elimika.course.api.dto.request;

import java.util.Date;

public record UpdateClassRequestDTO(
        String name,

        Date scheduledStartDate,

        Date scheduledEndDate,

        Long instructorId,

        Long availabilitySlotId
) {
}

