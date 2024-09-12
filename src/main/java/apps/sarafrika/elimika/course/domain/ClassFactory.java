package apps.sarafrika.elimika.course.domain;

import apps.sarafrika.elimika.course.api.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateClassRequestDTO;

public class ClassFactory {
    public static Class create(CreateClassRequestDTO createClassRequestDTO, Instructor instructor, Course course, InstructorAvailability availabilitySlot) {

        return Class.builder()
                .name(createClassRequestDTO.name())
                .scheduledStartDate(createClassRequestDTO.scheduledStartDate())
                .scheduledEndDate(createClassRequestDTO.scheduledEndDate())
                .instructor(instructor)
                .course(course)
                .availabilitySlot(availabilitySlot)
                .build();
    }

    public static void update(UpdateClassRequestDTO updateClassRequestDTO, Class classEntity) {

        classEntity.setName(updateClassRequestDTO.name());
        classEntity.setScheduledStartDate(updateClassRequestDTO.scheduledStartDate());
        classEntity.setScheduledEndDate(updateClassRequestDTO.scheduledEndDate());
    }
}
