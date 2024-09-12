package apps.sarafrika.elimika.course.api.dto.response;

import apps.sarafrika.elimika.course.domain.Class;

import java.util.Date;

public record ClassResponseDTO(
        Long id,

        String name,

        Date scheduledStartDate,

        Date scheduledEndDate
) {
    public static ClassResponseDTO from(Class classEntity) {

        return new ClassResponseDTO(
                classEntity.getId(),
                classEntity.getName(),
                classEntity.getScheduledStartDate(),
                classEntity.getScheduledEndDate()
        );
    }
}
