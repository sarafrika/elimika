package apps.sarafrika.elimika.course.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class CourseDeletedEvent extends ApplicationEvent {
    private final UUID courseUuid;

    public CourseDeletedEvent(Object source, UUID courseUuid) {
        super(source);
        this.courseUuid = courseUuid;
    }
}