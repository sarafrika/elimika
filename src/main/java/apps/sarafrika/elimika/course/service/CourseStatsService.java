package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseStatsDTO;

import java.util.UUID;

/**
 * Assembles a course's statistics for whoever is asking.
 */
public interface CourseStatsService {

    /**
     * Builds the statistics the <em>current caller</em> is entitled to.
     * <p>
     * The caller is read from the security context, never from a parameter, and each block is either
     * computed or left null so that Jackson drops it. A caller who is not an approved trainer is not
     * given a zeroed scoped block to interpret; they are given no scoped block at all.
     *
     * @param courseUuid the course to report on
     * @return the statistics, with only the entitled blocks populated
     * @throws apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException if the course does not exist
     */
    CourseStatsDTO getCourseStats(UUID courseUuid);
}
