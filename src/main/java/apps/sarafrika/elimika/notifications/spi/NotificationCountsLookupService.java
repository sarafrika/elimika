package apps.sarafrika.elimika.notifications.spi;

import java.util.Collection;
import java.util.UUID;

/** Unread notification counts for every dashboard domain of one recipient, in a single query. */
public interface NotificationCountsLookupService {

    /** Domains in {@code domains} always appear in the result, with zero counts when nothing is unread. */
    UnreadNotificationSummary summarizeUnread(UUID recipientUuid, Collection<String> domains);
}
