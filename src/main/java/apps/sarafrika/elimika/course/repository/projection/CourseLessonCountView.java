package apps.sarafrika.elimika.course.repository.projection;

import java.util.UUID;

/** Number of lessons in one course, from a single grouped count over a page of courses. */
public record CourseLessonCountView(UUID courseUuid, long lessonCount) {
}
