package apps.sarafrika.elimika.course.event.listener;

import apps.sarafrika.elimika.assessment.event.CreateAssessmentEvent;
import apps.sarafrika.elimika.assessment.event.UpdateAssessmentEvent;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResponseDTO;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AssessmentEventListener {

    private final CourseService courseService;
    private final LessonService lessonService;

    @EventListener
    void onAssessmentCreationEvent(CreateAssessmentEvent event) {

        if (event.createAssessmentRequestDTO().lessonId() != null) {

            final ResponseDTO<LessonResponseDTO> lesson = lessonService.findLesson(event.createAssessmentRequestDTO().courseId(), event.createAssessmentRequestDTO().lessonId());
            event.assessment().setLessonId(lesson.data().id());
        }

        final ResponseDTO<CourseResponseDTO> course = courseService.findCourse(event.createAssessmentRequestDTO().courseId());
        event.assessment().setCourseId(course.data().id());
    }

    @EventListener
    void onAssessmentUpdateEvent(UpdateAssessmentEvent event) {

        if (event.updateAssessmentRequestDTO().lessonId() != null) {

            final ResponseDTO<LessonResponseDTO> lesson = lessonService.findLesson(event.assessment().getCourseId(), event.updateAssessmentRequestDTO().lessonId());
            event.assessment().setLessonId(lesson.data().id());
        } else {

            event.assessment().setLessonId(null);
        }

        final ResponseDTO<CourseResponseDTO> course = courseService.findCourse(event.assessment().getCourseId());
        event.assessment().setCourseId(course.data().id());
    }
}
