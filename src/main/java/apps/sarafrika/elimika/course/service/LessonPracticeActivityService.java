package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonPracticeActivityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LessonPracticeActivityService {

    LessonPracticeActivityDTO createPracticeActivity(UUID courseUuid, UUID lessonUuid, LessonPracticeActivityDTO activityDTO);

    LessonPracticeActivityDTO getPracticeActivity(UUID courseUuid, UUID lessonUuid, UUID activityUuid);

    Page<LessonPracticeActivityDTO> getPracticeActivitiesByLesson(UUID courseUuid, UUID lessonUuid, Pageable pageable);

    LessonPracticeActivityDTO updatePracticeActivity(UUID courseUuid, UUID lessonUuid, UUID activityUuid, LessonPracticeActivityDTO activityDTO);

    void deletePracticeActivity(UUID courseUuid, UUID lessonUuid, UUID activityUuid);

    void reorderPracticeActivities(UUID courseUuid, UUID lessonUuid, List<UUID> activityUuids);
}
