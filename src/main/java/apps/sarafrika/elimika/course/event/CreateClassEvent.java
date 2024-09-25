package apps.sarafrika.elimika.course.event;

import apps.sarafrika.elimika.course.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.persistence.Class;

public record CreateClassEvent(Class classEntity, CreateClassRequestDTO createClassRequestDTO) {

}
