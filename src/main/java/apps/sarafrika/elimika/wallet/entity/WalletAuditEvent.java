package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.util.converter.WalletAuditEventTypeConverter;
import apps.sarafrika.elimika.wallet.util.converter.WalletOwnerTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One append-only entry in a wallet's audit trail. */
@Entity
@Immutable
@Table(name = "wallet_audit_events")
@Getter
@Setter
@NoArgsConstructor
public class WalletAuditEvent extends BaseEntity {

    @Convert(converter = WalletOwnerTypeConverter.class)
    @Column(name = "owner_type")
    private WalletOwnerType ownerType;

    @Column(name = "owner_uuid")
    private UUID ownerUuid;

    @Column(name = "currency_code")
    private String currencyCode;

    @Convert(converter = WalletAuditEventTypeConverter.class)
    @Column(name = "event_type")
    private WalletAuditEventType eventType;

    @Column(name = "actor_user_uuid")
    private UUID actorUserUuid;

    @Column(name = "reference")
    private String reference;

    @Column(name = "reason")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_values")
    private Map<String, Object> previousValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "updated_values")
    private Map<String, Object> updatedValues;

    public static WalletAuditEvent of(WalletOwner owner, WalletAuditEventType type, UUID actorUserUuid, String reference,
                                      String reason, Map<String, Object> previousValues,
                                      Map<String, Object> updatedValues) {
        WalletAuditEvent event = new WalletAuditEvent();
        event.setOwnerType(owner.type());
        event.setOwnerUuid(owner.uuid());
        event.setEventType(type);
        event.setActorUserUuid(actorUserUuid);
        event.setReference(reference);
        event.setReason(reason);
        event.setPreviousValues(previousValues);
        event.setUpdatedValues(updatedValues);
        return event;
    }
}
