package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorEducationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseCreatorEducationService {
    CourseCreatorEducationDTO createCourseCreatorEducation(CourseCreatorEducationDTO dto);
    CourseCreatorEducationDTO getCourseCreatorEducationByUuid(UUID uuid);
    Page<CourseCreatorEducationDTO> getAllCourseCreatorEducation(Pageable pageable);
    CourseCreatorEducationDTO updateCourseCreatorEducation(UUID uuid, CourseCreatorEducationDTO dto);
    void deleteCourseCreatorEducation(UUID uuid);
    Page<CourseCreatorEducationDTO> search(Map<String, String> searchParams, Pageable pageable);
}
