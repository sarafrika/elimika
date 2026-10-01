package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseOpenClasses;

import java.util.UUID;

/**
 * The classes of a public course that a visitor can still join.
 */
public interface CourseOpenClassService {

    /**
     * The open classes of a course that is publicly visible (root, published, active and
     * admin-approved), cheapest first and then soonest start, with the lowest fee as
     * {@code price_from}.
     *
     * @param courseUuid the course
     * @return the open classes, possibly none
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException when the course does
     *         not exist or is not publicly visible, for every caller alike
     */
    CourseOpenClasses getOpenClasses(UUID courseUuid);
}
