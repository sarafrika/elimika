package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** A self-registration started in Elimika: requested domain, terms acceptance and email timing only. */
@Entity
@Table(name = "account_registrations")
@Getter
@Setter
@NoArgsConstructor
public class AccountRegistration extends BaseEntity {

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "requested_domain")
    private String requestedDomain;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "actions_email_sent_at")
    private LocalDateTime actionsEmailSentAt;
}
