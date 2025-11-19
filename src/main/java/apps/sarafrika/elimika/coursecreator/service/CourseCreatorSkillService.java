package apps.sarafrika.elimika.coursecreator.service;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorSkillDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CourseCreatorSkillService {
    CourseCreatorSkillDTO createCourseCreatorSkill(CourseCreatorSkillDTO dto);
    CourseCreatorSkillDTO getCourseCreatorSkillByUuid(UUID uuid);
    Page<CourseCreatorSkillDTO> getAllCourseCreatorSkills(Pageable pageable);
    CourseCreatorSkillDTO updateCourseCreatorSkill(UUID uuid, CourseCreatorSkillDTO dto);
    void deleteCourseCreatorSkill(UUID uuid);
    Page<CourseCreatorSkillDTO> search(Map<String, String> searchParams, Pageable pageable);
}
