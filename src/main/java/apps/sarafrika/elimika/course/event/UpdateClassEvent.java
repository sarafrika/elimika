package apps.sarafrika.elimika.course.event;

import apps.sarafrika.elimika.course.dto.request.UpdateClassRequestDTO;
import apps.sarafrika.elimika.course.persistence.Class;

public record UpdateClassEvent(Class classEntity, UpdateClassRequestDTO updateClassRequestDTO) {

}
