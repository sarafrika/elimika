package apps.sarafrika.elimika.course.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class CourseCreatedEvent extends ApplicationEvent {
    private final UUID courseUuid;

    public CourseCreatedEvent(Object source, UUID courseUuid) {
        super(source);
        this.courseUuid = courseUuid;
    }
}