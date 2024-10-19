package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface LessonService {

    ResponseDTO<LessonResponseDTO> findLesson(Long courseId, Long lessonId);

    ResponsePageableDTO<LessonResponseDTO> findAllLessons(Long courseId, Pageable pageable);

    ResponseDTO<Void> createLesson(Long courseId, CreateLessonRequestDTO createLessonRequestDTO);

    ResponseDTO<Void> updateLesson(Long courseId, UpdateLessonRequestDTO updateLessonRequestDTO, Long lessonId);

    void deleteLesson(Long courseId,  Long lessonId);

    ResponseDTO<LessonResponseDTO> findLessonById(Long lessonId);
}
