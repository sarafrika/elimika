package apps.sarafrika.elimika.course.spi;

import java.util.UUID;

/**
 * Published when a course's skill tags are replaced, so modules that copy or inherit them (the
 * marketplace job index) can refresh.
 */
public record CourseSkillsChangedEvent(UUID courseUuid) {
}
