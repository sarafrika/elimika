package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCertificationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseCreatorCertificationService {
    CourseCreatorCertificationDTO createCourseCreatorCertification(CourseCreatorCertificationDTO dto);
    CourseCreatorCertificationDTO getCourseCreatorCertificationByUuid(UUID uuid);
    Page<CourseCreatorCertificationDTO> getAllCourseCreatorCertifications(Pageable pageable);
    CourseCreatorCertificationDTO updateCourseCreatorCertification(UUID uuid, CourseCreatorCertificationDTO dto);
    void deleteCourseCreatorCertification(UUID uuid);
    Page<CourseCreatorCertificationDTO> search(Map<String, String> searchParams, Pageable pageable);
}
