package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;

import java.util.Locale;
import java.util.Map;

/**
 * The dashboard a caller is standing on while they make a request — the hat they are wearing, as
 * opposed to the hats they own.
 * <p>
 * {@link DomainSecurityService} answers "what domains does this person hold?", which is a fact about
 * the account and the same on every page. It is the wrong question for a platform where one account
 * is routinely an administrator, an instructor and a learner at once: asked on the learner's own
 * course page, it reports the administrator, and the page is then built for an administrator. This
 * enum carries the missing half — which of those domains the request was made from.
 * <p>
 * <strong>It may only narrow.</strong> Nothing here grants anything. Every value is a request to be
 * treated as <em>less</em> than the caller could be, and each is still paired with the real
 * entitlement check it caps: {@link #ADMIN} restricts a caller to an administrator's footing but
 * {@code isPlatformAdmin()} still has to hold, so a forged header buys its sender nothing it did not
 * already have. A value that arrives unrecognised — or does not arrive at all, as from a
 * server-to-server caller or an older client — is {@link #UNSPECIFIED} and caps nothing, because an
 * absent header must never be read as a claim.
 * <p>
 * The wire vocabulary is {@link UserDomain}'s, so a client sends back the same string the platform
 * gave it. {@code organisation} and {@code organization} are accepted alongside
 * {@code organisation_user} only because the frontend's own domain list already carries them.
 */
public enum ActingDomain {

    /** Reading as a learner. Entitled to a course only through an enrolment. */
    STUDENT(UserDomain.student),
    /** Reading as an individual trainer. Entitled through a personal training approval. */
    INSTRUCTOR(UserDomain.instructor),
    /** Reading on an organisation's behalf. Entitled through the organisation's approval. */
    ORGANISATION(UserDomain.organisation_user),
    /** Reading as an author. Entitled to what they wrote, and to nothing else on that footing. */
    COURSE_CREATOR(UserDomain.course_creator),
    /** Reading as a platform administrator — subject to {@code isPlatformAdmin()} still holding. */
    ADMIN(UserDomain.admin),
    /** Reading as a guardian. A guardian's own footing on a course is a browser's. */
    PARENT(UserDomain.parent),
    /** No claim was made. Behaves exactly as the platform did before acting domains existed. */
    UNSPECIFIED(null);

    private static final Map<String, ActingDomain> BY_WIRE_VALUE = Map.ofEntries(
            Map.entry(UserDomain.student.name(), STUDENT),
            Map.entry(UserDomain.instructor.name(), INSTRUCTOR),
            Map.entry(UserDomain.organisation_user.name(), ORGANISATION),
            Map.entry(UserDomain.course_creator.name(), COURSE_CREATOR),
            Map.entry(UserDomain.admin.name(), ADMIN),
            Map.entry(UserDomain.parent.name(), PARENT),
            // The frontend's own domain list carries both spellings of the organisation dashboard.
            Map.entry("organisation", ORGANISATION),
            Map.entry("organization", ORGANISATION));

    private final UserDomain userDomain;

    ActingDomain(UserDomain userDomain) {
        this.userDomain = userDomain;
    }

    /** The {@link UserDomain} this dashboard corresponds to, or null for {@link #UNSPECIFIED}. */
    public UserDomain userDomain() {
        return userDomain;
    }

    /** Whether the caller made a claim at all. */
    public boolean isSpecified() {
        return this != UNSPECIFIED;
    }

    /**
     * Parses a header value, tolerating case and surrounding whitespace.
     * <p>
     * Anything else — a blank, an unknown word, a value from a client that predates this header —
     * is {@link #UNSPECIFIED}, never a guess and never a grant.
     *
     * @param value the raw header value, may be null
     * @return the dashboard claimed, or {@link #UNSPECIFIED}
     */
    public static ActingDomain fromHeader(String value) {
        if (value == null) {
            return UNSPECIFIED;
        }
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) {
            return UNSPECIFIED;
        }
        return BY_WIRE_VALUE.getOrDefault(normalised, UNSPECIFIED);
    }
}
