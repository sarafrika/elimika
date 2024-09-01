package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
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
