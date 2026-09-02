package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One organisation-originated notification broadcast (outgoing). The per-recipient in-app/email
 * notifications are created through the normal notification pipeline; this row is the organisation's
 * own record of what it sent, to which audience, on which channel, and how many recipients it reached.
 */
@Entity
@Table(name = "notification_dispatches")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDispatch extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "sender_user_uuid")
    private UUID senderUserUuid;

    @Column(name = "audience")
    private String audience;

    @Column(name = "channel")
    private String channel;

    @Column(name = "title")
    private String title;

    @Column(name = "body")
    private String body;

    @Column(name = "recipient_count")
    private Integer recipientCount;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
}
