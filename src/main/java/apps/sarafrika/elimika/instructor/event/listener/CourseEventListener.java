package apps.sarafrika.elimika.instructor.event.listener;

import apps.sarafrika.elimika.course.event.CreateCourseEvent;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseEventListener {

    private static final String ERROR_INSTRUCTOR_NOT_FOUND = "Instructor not found";

    private final InstructorService instructorService;

    @EventListener
    void onCourseCreationEvent(CreateCourseEvent event) {

        handleInstructorAssignmentForCourse(event);
    }

    private void handleInstructorAssignmentForCourse(CreateCourseEvent event) {

    }
}
