package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LessonContentService {
    LessonContentDTO createLessonContent(LessonContentDTO lessonContentDTO);

    LessonContentDTO getLessonContentByUuid(UUID uuid);

    Page<LessonContentDTO> getAllLessonContents(Pageable pageable);

    LessonContentDTO updateLessonContent(UUID uuid, LessonContentDTO lessonContentDTO);

    void deleteLessonContent(UUID uuid);

    Page<LessonContentDTO> search(Map<String, String> searchParams, Pageable pageable);

    List<LessonContentDTO> getContentByLesson(UUID lessonUuid);

    void reorderContent(UUID lessonUuid, List<UUID> contentUuids);
}