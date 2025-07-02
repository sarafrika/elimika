package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseRequirementDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseRequirementService {
    CourseRequirementDTO createCourseRequirement(CourseRequirementDTO courseRequirementDTO);

    CourseRequirementDTO getCourseRequirementByUuid(UUID uuid);

    Page<CourseRequirementDTO> getAllCourseRequirements(Pageable pageable);

    CourseRequirementDTO updateCourseRequirement(UUID uuid, CourseRequirementDTO courseRequirementDTO);

    void deleteCourseRequirement(UUID uuid);

    Page<CourseRequirementDTO> search(Map<String, String> searchParams, Pageable pageable);
}