package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.Class;

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
