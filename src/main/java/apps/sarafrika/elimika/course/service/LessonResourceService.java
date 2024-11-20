package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateLessonResourceRequestDTO;
import apps.sarafrika.elimika.course.dto.request.LessonResouceRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonResourceRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResourceResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;

import java.util.List;

public interface LessonResourceService {

    ResponseDTO<List<LessonResourceResponseDTO>> findLessonResources(LessonResouceRequestDTO lessonResouceRequestDTO);

    ResponseDTO<LessonResourceResponseDTO> findLessonResource(Long lessonResourceId);

    ResponseDTO<LessonResourceResponseDTO> createLessonResource(Long lessonId, CreateLessonResourceRequestDTO createLessonResourceRequestDTO);

    ResponseDTO<List<LessonResourceResponseDTO>> createLessonResources(Long lessonId, List<CreateLessonResourceRequestDTO> createLessonResourceRequestDTOs);

    ResponseDTO<LessonResourceResponseDTO> updateLessonResource(Long lessonResourceId, UpdateLessonResourceRequestDTO updateLessonResourceRequestDTO);

    ResponseDTO<List<LessonResourceResponseDTO>> updateLessonResources(Long lessonId, List<UpdateLessonResourceRequestDTO> updateLessonResourceRequestDTOs);

    void deleteLessonResource(Long lessonResourceId);
}
