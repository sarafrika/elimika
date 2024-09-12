package apps.sarafrika.elimika.course.api;

import apps.sarafrika.elimika.course.api.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    ResponseDTO<CourseResponseDTO> findById(Long id);

    ResponsePageableDTO<CourseResponseDTO> findAll(Pageable pageable);

    ResponseDTO<Void> create(CreateCourseRequestDTO createCourseRequestDTO);

    ResponseDTO<Void> update(UpdateCourseRequestDTO updateCourseRequestDTO, Long id);

    void delete(Long id);
}
