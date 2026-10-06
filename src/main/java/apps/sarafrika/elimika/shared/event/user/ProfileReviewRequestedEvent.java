package apps.sarafrika.elimika.shared.event.user;

import java.util.UUID;

/** A profile (e.g. a course creator's skills wallet) was submitted and awaits platform admin review. */
public record ProfileReviewRequestedEvent(
        UUID userUuid,
        String userDomain
) {
}
