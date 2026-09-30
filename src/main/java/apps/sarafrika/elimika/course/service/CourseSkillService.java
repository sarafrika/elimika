package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseSkillDTO;
import apps.sarafrika.elimika.course.dto.CourseSkillsUpdateRequest;

import java.util.List;
import java.util.UUID;

/** A course's skill tags: optional, chosen by the course's creator from the skills taxonomy. */
public interface CourseSkillService {

    /** The tags of a course the caller may read (404 otherwise), heaviest first. */
    List<CourseSkillDTO> getCourseSkills(UUID courseUuid);

    /** Replaces the course's tags with the given list. The caller's ownership is checked at the route. */
    List<CourseSkillDTO> replaceCourseSkills(UUID courseUuid, CourseSkillsUpdateRequest request);
}
