package apps.sarafrika.elimika.course.dto.request;

import java.util.Date;

public record CreateClassRequestDTO(
        String name,

        Date scheduledStartDate,

        Date scheduledEndDate,

        Long instructorId,

        Long courseId,

        Long instructorAvailabilityId
) {
}

