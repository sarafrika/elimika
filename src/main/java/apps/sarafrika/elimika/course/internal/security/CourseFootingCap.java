package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import apps.sarafrika.elimika.shared.security.ActingDomainCap;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Which footings a course may be read on from the dashboard the caller is standing on.
 * <p>
 * The platform used to answer "what is the highest footing this <em>person</em> holds", which is the
 * same answer on every page. A user who administers the platform and is also a learner therefore
 * read every course as an administrator, including ones they had never enrolled in and were looking
 * at from their own learner dashboard: full lesson bodies, the owner statistics block with gross
 * sales and the platform fee, the trainers' rate cards, the named roster of everybody enrolled. This
 * class supplies the missing half of the question — what they are acting as right now — and every
 * entitlement decision the course record makes passes through it.
 * <p>
 * <strong>Only ever a narrowing.</strong> Each answer below says a footing may still be
 * <em>reached</em>, never that it is granted. Reaching one is a conjunction: the dashboard must
 * permit it <em>and</em> the real check behind it — ownership, platform-admin, an approved training
 * application, an enrolment — must independently pass. So the worst a forged header achieves is to
 * hand its sender less than they already had.
 * <p>
 * Each footing is expressed as the {@link UserDomain} it belongs to and delegated to
 * {@link ActingDomainCap}, so there is one rule about what a dashboard may act through rather than
 * a course-shaped copy of it. Only the three footings with no domain of their own are decided here.
 */
@Component
@RequiredArgsConstructor
public class CourseFootingCap {

    /** The domains that make a caller staff on a course rather than an outsider or a learner. */
    private static final UserDomain[] STAFF_DOMAINS = {
            UserDomain.course_creator, UserDomain.admin,
            UserDomain.organisation_user, UserDomain.instructor};

    private final ActingDomainCap actingDomainCap;

    /**
     * Whether the caller's dashboard may reach {@code footing} at all.
     * <p>
     * A false answer means the rung is skipped and resolution continues down the ladder — it is a
     * cap, not a denial, so a student-acting administrator who <em>is</em> enrolled still lands on
     * {@code STUDENT} rather than on nothing.
     * <p>
     * The three footings without a domain of their own:
     * <ul>
     *   <li>{@code PROSPECT} is the floor every dashboard falls back to, so nothing may cap it.</li>
     *   <li>{@code PENDING} — an application under review — belongs to whichever of the two kinds of
     *       applicant filed it, so either of those dashboards may report it and no other. A
     *       learner's page has nothing to say about an application to teach.</li>
     *   <li>{@code APPLICANT} describes somebody who holds a teaching domain and has not applied,
     *       which is a fact about a person rather than about a school.</li>
     * </ul>
     *
     * @param footing the rung being considered
     * @return true when this dashboard is allowed to stand on it
     */
    public boolean permits(CourseContentAccess footing) {
        if (footing == null) {
            return false;
        }
        return switch (footing) {
            case PROSPECT -> true;
            case CREATOR -> actingDomainCap.permits(UserDomain.course_creator);
            case ADMIN -> actingDomainCap.permits(UserDomain.admin);
            case ORGANISATION -> actingDomainCap.permits(UserDomain.organisation_user);
            case INSTRUCTOR -> actingDomainCap.permits(UserDomain.instructor);
            case STUDENT -> actingDomainCap.permits(UserDomain.student);
            case PENDING -> actingDomainCap.permitsAny(UserDomain.instructor, UserDomain.organisation_user);
            case APPLICANT -> actingDomainCap.permits(UserDomain.instructor);
        };
    }

    /**
     * Whether the caller's dashboard is one that can carry a course's commercial figures — the owner
     * statistics block, the trainers' rate cards, the applications they negotiated.
     * <p>
     * All of those are read today by "the course owner or a platform administrator", a test that
     * consults no footing at all, which is why capping the content resolver alone left gross sales
     * on the learner's page. The two clauses are capped separately so that each keeps its own real
     * check: a caller acting as a creator still has to own the course, and one acting as an
     * administrator still has to be one.
     *
     * @param courseOwner   whether the caller actually authored the course
     * @param platformAdmin whether the caller actually administers the platform
     * @return true when this dashboard may be shown the money
     */
    public boolean permitsCommercials(boolean courseOwner, boolean platformAdmin) {
        return (courseOwner && permits(CourseContentAccess.CREATOR))
                || (platformAdmin && permits(CourseContentAccess.ADMIN));
    }

    /**
     * Whether the caller's dashboard may stand on any staff footing.
     * <p>
     * Backs the coarse "is this caller staff rather than a learner?" bypasses — the one that lets
     * drafts and unpublished material through, and the one that opens the named enrolment roster. On
     * a learner's or a guardian's dashboard the answer is no, and the caller falls back to the
     * enrolment test that everybody else takes.
     */
    public boolean permitsAnyStaffFooting() {
        return actingDomainCap.permitsAny(STAFF_DOMAINS);
    }
}
