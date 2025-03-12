package apps.sarafrika.elimika.course.event;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.persistence.Course;
import lombok.Getter;

@Getter
public class CreateCourseEvent {

    private final Course course;
    private final CreateCourseRequestDTO createCourseRequestDTO;

    public CreateCourseEvent(Course course, CreateCourseRequestDTO createCourseRequestDTO) {
        this.course = course;
        this.createCourseRequestDTO = createCourseRequestDTO;
    }
}
