package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    ResponseDTO<Void> createCourse(CreateCourseRequestDTO createCourseRequestDTO);

    ResponseDTO<CourseResponseDTO> findCourse(Long id);

    ResponsePageableDTO<CourseResponseDTO> findAllCourses(Pageable pageable);

    ResponseDTO<Void> updateCourse(UpdateCourseRequestDTO updateCourseRequestDTO, Long id);

    void deleteCourse(Long id);
}
