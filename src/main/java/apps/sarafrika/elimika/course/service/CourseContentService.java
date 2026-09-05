package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.OrganisationCourseContentDTO;

import java.util.UUID;

/**
 * Assembles a course's content for whoever asked for it, gating the lesson bodies behind the
 * caller's resolved access so a viewer without full read rights receives an outline and nothing
 * more.
 * <p>
 * The gate is applied here, at assembly, rather than trusted to the client: a payload that reaches
 * the browser has already left the building, so content the viewer may not read is never put into
 * one.
 */
public interface CourseContentService {

    /**
     * Resolves the caller's own footing on the course and assembles accordingly.
     *
     * @param courseUuid the course being viewed
     * @return outline, rating and the caller's {@code access}; lesson content only when that access
     * carries full read rights
     */
    OrganisationCourseContentDTO getContentForCaller(UUID courseUuid);

    /**
     * The same assembly, scoped to a named organisation rather than to the caller.
     *
     * @param courseUuid       the course being viewed
     * @param organisationUuid the organisation the content is scoped to
     * @return outline + rating always; full lesson content only when the organisation is approved
     * @deprecated superseded by {@link #getContentForCaller(UUID)}, which serves every viewer —
     * including a course creator, who has no organisation to scope by. Kept so the
     * organisation-scoped route stays answerable while clients migrate.
     */
    @Deprecated(since = "2.142.0", forRemoval = true)
    OrganisationCourseContentDTO getContentForOrganisation(UUID courseUuid, UUID organisationUuid);
}
