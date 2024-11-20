package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LessonService {

    ResponseDTO<LessonResponseDTO> findLesson(Long courseId, Long lessonId);

    ResponsePageableDTO<LessonResponseDTO> findAllLessons(Long courseId, Pageable pageable);

    ResponseDTO<LessonResponseDTO> createLesson(Long courseId, CreateLessonRequestDTO createLessonRequestDTO, List<MultipartFile> files);

    ResponseDTO<Void> updateLesson(Long courseId, UpdateLessonRequestDTO updateLessonRequestDTO, Long lessonId);

    void deleteLesson(Long courseId,  Long lessonId);

    ResponseDTO<LessonResponseDTO> findLessonById(Long lessonId);
}
