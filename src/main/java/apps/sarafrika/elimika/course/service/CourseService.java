package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    CourseResponseDTO findById(Long id);

    ResponsePageableDTO<CourseResponseDTO> findAll(Pageable pageable);

    void create(CreateCourseRequestDTO course);

    CourseResponseDTO update(Course course);

    void delete(int id);
}
