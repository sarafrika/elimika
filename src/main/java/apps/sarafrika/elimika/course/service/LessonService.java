package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface LessonService {

    ResponseDTO<LessonResponseDTO> findLesson(Long id);

    ResponsePageableDTO<LessonResponseDTO> findAllLessons(Long classId, Pageable pageable);

    ResponseDTO<Void> createLesson(CreateLessonRequestDTO createLessonRequestDTO);

    ResponseDTO<Void> updateLesson(UpdateLessonRequestDTO updateLessonRequestDTO, Long id);

    void deleteLesson(Long id);
}
