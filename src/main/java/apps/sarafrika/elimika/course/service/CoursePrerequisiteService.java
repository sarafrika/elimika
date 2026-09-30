package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CoursePrerequisiteDTO;
import apps.sarafrika.elimika.course.dto.CoursePrerequisitesRequest;

import java.util.List;
import java.util.UUID;

/**
 * Structured course-to-course prerequisites, edited as one set in the course editor.
 */
public interface CoursePrerequisiteService {

    /**
     * The course's prerequisites, for anyone who may read the course.
     *
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException when the course does not
     *         exist or the caller may not read it
     */
    List<CoursePrerequisiteDTO> getPrerequisites(UUID courseUuid);

    /**
     * Replaces the course's prerequisite set. On a live, approved course the change lands on its draft
     * and goes through review like any other authoring edit; otherwise it applies directly.
     *
     * @return the resulting set, read from the course the write landed on
     * @throws IllegalArgumentException for a self-reference, a duplicate, a cycle, or a prior course that
     *         is neither published nor the author's own
     */
    List<CoursePrerequisiteDTO> replacePrerequisites(UUID courseUuid, CoursePrerequisitesRequest request);
}
