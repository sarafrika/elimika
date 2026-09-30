package apps.sarafrika.elimika.course.spi;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The skills courses are tagged with, for modules that inherit them (a marketplace job with no
 * skill tags of its own takes its course's).
 */
public interface CourseSkillLookupService {

    /** Each course's skill tags, heaviest first. Courses without tags are absent. */
    Map<UUID, List<CourseSkillTag>> findSkillsByCourseUuids(Collection<UUID> courseUuids);

    /**
     * @param skillUuid the skills-taxonomy entry
     * @param level     the level the course teaches it to
     * @param weight    1-5, how central the skill is to the course
     */
    record CourseSkillTag(UUID skillUuid, ProficiencyLevel level, int weight) {
    }
}
