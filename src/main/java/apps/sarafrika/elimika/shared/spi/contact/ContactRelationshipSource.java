package apps.sarafrika.elimika.shared.spi.contact;

import java.util.UUID;

/**
 * One module's answer to "does this caller have a working relationship with this person".
 * <p>
 * A user's email, phone number and date of birth are not directory data, but withholding them from
 * everyone who is not the account holder or an administrator breaks the product: an instructor has
 * to be able to reach a learner waitlisted on their own class, and a course creator has to be able
 * to reach an instructor applying to teach their course. Those relationships live in the modules
 * that own the join tables — {@code timetabling} owns enrolments, {@code course} owns enrolments and
 * training applications — so each contributes an implementation of this interface rather than
 * exporting its tables.
 * <p>
 * The interface lives in {@code shared} because {@code shared} is the only module every other one
 * may depend on, which is what keeps the dependency pointing inwards. Implementations are consulted
 * by {@code UserContactSecurityService}, which stops at the first {@code true}, so an implementation
 * that cannot decide must return {@code false} rather than throw: an unreachable module denies
 * contact details, it does not fail the request.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
public interface ContactRelationshipSource {

    /**
     * A person, addressed by every identifier a relationship might be keyed on.
     * <p>
     * Enrolments are keyed by student UUID, class instances and training applications by instructor
     * UUID, and organisation membership by user UUID; resolving all three once in the caller keeps
     * each implementation from repeating the same two lookups. A component is {@code null} when the
     * person holds no such profile.
     *
     * @param userUuid       the account identifier, never null for a resolved party
     * @param studentUuid    the party's student profile, or null when they have none
     * @param instructorUuid the party's instructor profile, or null when they have none
     */
    record Party(UUID userUuid, UUID studentUuid, UUID instructorUuid) {

        /** True when this party holds no profile a relationship could be keyed on. */
        public boolean hasNoProfile() {
            return studentUuid == null && instructorUuid == null;
        }
    }

    /**
     * Whether the viewer stands in a relationship to the subject that justifies showing the
     * subject's contact details. Directional on purpose: an instructor may reach their learner,
     * which says nothing about one learner reaching another.
     *
     * @param viewer  the authenticated caller
     * @param subject the person whose record is being read
     * @return true when this module knows of such a relationship; false when it does not, or cannot tell
     */
    boolean viewerMayContactSubject(Party viewer, Party subject);
}
