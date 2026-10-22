package apps.sarafrika.elimika.shared.otp;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One issued code: only its keyed hash is kept, never the digits. */
@Entity
@Table(name = "one_time_codes")
@Getter
@Setter
@NoArgsConstructor
public class OneTimeCode extends BaseEntity {

    @Convert(converter = OneTimeCodePurposeConverter.class)
    @Column(name = "purpose")
    private OneTimeCodePurpose purpose;

    @Column(name = "subject_uuid")
    private UUID subjectUuid;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "code_hash")
    private String codeHash;

    @Column(name = "sent_to")
    private String sentTo;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "max_attempts")
    private int maxAttempts;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "invalidated_at")
    private OffsetDateTime invalidatedAt;

    public boolean isLive(OffsetDateTime now) {
        return consumedAt == null && invalidatedAt == null && expiresAt.isAfter(now) && attempts < maxAttempts;
    }
}
