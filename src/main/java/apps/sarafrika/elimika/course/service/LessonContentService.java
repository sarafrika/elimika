package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateLessonContentDTO;
import apps.sarafrika.elimika.course.dto.request.LessonContentRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonContentDTO;
import apps.sarafrika.elimika.course.dto.response.LessonContentResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LessonContentService {

    ResponseDTO<List<LessonContentResponseDTO>> findAllLessonContent(LessonContentRequestDTO lessonContentRequestDTO);

    ResponseDTO<List<LessonContentResponseDTO>> createLessonContent(Long lessonId, List<CreateLessonContentDTO> metadata, List<MultipartFile> files);

    ResponseDTO<LessonContentResponseDTO> updateLessonContent(Long lessonContentId, UpdateLessonContentDTO updateLessonContentDTO);

    void deleteLessonContent(Long lessonContentId);
}
