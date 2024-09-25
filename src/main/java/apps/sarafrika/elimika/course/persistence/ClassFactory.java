package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateClassRequestDTO;

public class ClassFactory {

    public static Class create(CreateClassRequestDTO createClassRequestDTO) {

        return Class.builder()
                .name(createClassRequestDTO.name())
                .scheduledStartDate(createClassRequestDTO.scheduledStartDate())
                .scheduledEndDate(createClassRequestDTO.scheduledEndDate())
                .build();
    }

    public static void update(UpdateClassRequestDTO updateClassRequestDTO, Class classEntity) {

        classEntity.setName(updateClassRequestDTO.name());
        classEntity.setScheduledStartDate(updateClassRequestDTO.scheduledStartDate());
        classEntity.setScheduledEndDate(updateClassRequestDTO.scheduledEndDate());
    }
}
