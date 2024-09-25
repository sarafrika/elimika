package apps.sarafrika.elimika.instructor.event.listener;

import apps.sarafrika.elimika.course.event.CreateCourseEvent;
import apps.sarafrika.elimika.instructor.config.exception.InstructorNotFoundException;
import apps.sarafrika.elimika.instructor.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

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

        final Set<Long> instructorIds = event.getCreateCourseRequestDTO().instructorIds();

        if (instructorIds == null || instructorIds.isEmpty()) {
            return;
        }

        final ResponseDTO<Set<InstructorResponseDTO>> foundInstructors = instructorService.findInstructorsByIds(instructorIds);

        final Set<Long> missingInstructorIds = instructorIds.stream()
                .filter(instructorId -> foundInstructors.data().stream().noneMatch(foundInstructor -> foundInstructor.id().equals(instructorId)))
                .collect(Collectors.toSet());

        if (!missingInstructorIds.isEmpty()) {

            throw new InstructorNotFoundException(ERROR_INSTRUCTOR_NOT_FOUND);
        }

        event.getCourse().setInstructorIds(foundInstructors.data().stream().map(InstructorResponseDTO::id).collect(Collectors.toSet()));
    }
}
