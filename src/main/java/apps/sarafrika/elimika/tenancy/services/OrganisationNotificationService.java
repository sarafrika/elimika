package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.NotificationDispatchDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationNotificationRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Organisation-originated (outgoing) notifications: broadcasting a message to an audience of the
 * organisation's members, and reading back what the organisation has sent.
 */
public interface OrganisationNotificationService {

    /**
     * Broadcast a notification to the requested audience of the organisation's members. Each
     * recipient receives an in-app notification (and an email when the channel is email). Records and
     * returns the dispatch.
     *
     * @param organisationUuid the sending organisation
     * @param request          audience, channel, title and message
     * @param senderUserUuid   the acting user, recorded on the dispatch (may be null)
     */
    NotificationDispatchDTO send(UUID organisationUuid, SendOrganisationNotificationRequestDTO request,
                                 UUID senderUserUuid);

    /** The organisation's outgoing broadcasts, newest first, capped at {@code limit}. */
    List<NotificationDispatchDTO> listSent(UUID organisationUuid, int limit);
}
