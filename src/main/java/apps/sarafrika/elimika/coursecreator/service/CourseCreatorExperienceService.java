package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorExperienceDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseCreatorExperienceService {
    CourseCreatorExperienceDTO createCourseCreatorExperience(CourseCreatorExperienceDTO dto);
    CourseCreatorExperienceDTO getCourseCreatorExperienceByUuid(UUID uuid);
    CourseCreatorExperienceDTO updateCourseCreatorExperience(UUID uuid, CourseCreatorExperienceDTO dto);
    void deleteCourseCreatorExperience(UUID courseCreatorUuid, UUID uuid);
    Page<CourseCreatorExperienceDTO> search(Map<String, String> searchParams, Pageable pageable);
}
