package apps.sarafrika.elimika.course.util.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * The footing a caller views a course's content on.
 * <p>
 * One value, resolved server-side, replaces every client-side attempt to guess what a viewer may
 * read. The client renders the state it is told; it never re-derives it from the signed-in user's
 * domain, because only the server knows whether a training application was approved, whether an
 * enrolment still allows access, or whether an approval has since been revoked.
 * <p>
 * Not persisted — this is a computed view of a request, so there is deliberately no
 * {@code AttributeConverter} for it.
 */
public enum CourseContentAccess {

    /** The course creator who authored it. */
    CREATOR("creator"),
    /** A platform administrator. */
    ADMIN("admin"),
    /** A member of an organisation approved to train the course. */
    ORGANISATION("organisation"),
    /** An instructor personally approved to train the course. */
    INSTRUCTOR("instructor"),
    /** Holds a teaching domain but has no application on file for this course. */
    APPLICANT("applicant"),
    /** Has applied — personally or through their organisation — and is not approved. */
    PENDING("pending"),
    /** Everyone else, including anonymous browsers of the catalogue. */
    PROSPECT("prospect"),
    /** A learner with an access-allowing enrolment on the course. */
    STUDENT("student");

    private final String value;
    private static final Map<String, CourseContentAccess> VALUE_MAP = new HashMap<>();

    static {
        for (CourseContentAccess access : CourseContentAccess.values()) {
            VALUE_MAP.put(access.value, access);
            VALUE_MAP.put(access.value.toUpperCase(), access);
        }
    }

    CourseContentAccess(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonCreator
    public static CourseContentAccess fromValue(String value) {
        CourseContentAccess access = VALUE_MAP.get(value);
        if (access == null) {
            throw new IllegalArgumentException("Unknown CourseContentAccess: " + value);
        }
        return access;
    }

    /**
     * Whether lesson bodies may be transmitted to this viewer at all.
     * <p>
     * The single place the rule lives. A viewer this returns {@code false} for receives a payload
     * with no content items and no lesson identifiers — not a full payload the client is trusted to
     * hide, because a payload that reaches the browser has already left the building.
     */
    public boolean grantsFullContent() {
        return switch (this) {
            case CREATOR, ADMIN, ORGANISATION, INSTRUCTOR, STUDENT -> true;
            case APPLICANT, PENDING, PROSPECT -> false;
        };
    }

    /**
     * Whether this viewer sees lessons that are still drafts.
     * <p>
     * A separate question from {@link #grantsFullContent()}, and deliberately so. A learner is
     * enrolled and therefore reads the whole course, but reads the course as it was published:
     * unfinished material is the author's business until they say otherwise. The footings that
     * build or deliver the course — the creator, an admin moderating it, and the trainers approved
     * to teach it — need the drafts precisely because they are the ones preparing them.
     */
    public boolean seesDrafts() {
        return switch (this) {
            case CREATOR, ADMIN, ORGANISATION, INSTRUCTOR -> true;
            case STUDENT, APPLICANT, PENDING, PROSPECT -> false;
        };
    }
}
