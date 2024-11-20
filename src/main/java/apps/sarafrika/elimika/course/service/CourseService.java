package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CourseService {

    ResponseDTO<CourseResponseDTO> createCourse(CreateCourseRequestDTO createCourseRequestDTO, MultipartFile thumbnail);

    ResponseDTO<CourseResponseDTO> findCourse(Long courseId);

    ResponsePageableDTO<CourseResponseDTO> findAllCourses(Pageable pageable);

    ResponseDTO<CourseResponseDTO> updateCourse(UpdateCourseRequestDTO updateCourseRequestDTO, Long courseId);

    void deleteCourse(Long courseId);
}
