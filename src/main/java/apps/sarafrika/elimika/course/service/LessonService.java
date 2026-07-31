package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface LessonService {
    LessonDTO createLesson(LessonDTO lessonDTO);

    LessonDTO getLessonByUuid(UUID uuid);

    Page<LessonDTO> getAllLessons(Pageable pageable);

    LessonDTO updateLesson(UUID uuid, LessonDTO lessonDTO);

    void deleteLesson(UUID uuid);

    Page<LessonDTO> search(Map<String, String> searchParams, Pageable pageable);

    /**
     * A course's lessons as the caller is entitled to see them: staff get drafts too, learners get
     * only published, active lessons.
     * <p>
     * Listing drafts to a learner is worse than useless — every downstream detail and assessment
     * endpoint refuses them, so an unfiltered list advertises material that 404s on click.
     */
    Page<LessonDTO> getCourseLessonsForCaller(UUID courseUuid, Pageable pageable);
}