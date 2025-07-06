package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonProgressDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface LessonProgressService {
    LessonProgressDTO createLessonProgress(LessonProgressDTO lessonProgressDTO);

    LessonProgressDTO getLessonProgressByUuid(UUID uuid);

    Page<LessonProgressDTO> getAllLessonProgresses(Pageable pageable);

    LessonProgressDTO updateLessonProgress(UUID uuid, LessonProgressDTO lessonProgressDTO);

    void deleteLessonProgress(UUID uuid);

    Page<LessonProgressDTO> search(Map<String, String> searchParams, Pageable pageable);
}