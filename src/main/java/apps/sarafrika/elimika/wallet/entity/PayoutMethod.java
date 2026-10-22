package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.util.converter.PayoutMethodStatusConverter;
import apps.sarafrika.elimika.wallet.util.converter.PayoutMethodTypeConverter;
import apps.sarafrika.elimika.wallet.util.converter.WalletOwnerTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Where a wallet's withdrawals can go: an M-Pesa number of a user or an organisation. */
@Entity
@Table(name = "payout_methods")
@Getter
@Setter
@NoArgsConstructor
public class PayoutMethod extends BaseEntity {

    @Convert(converter = WalletOwnerTypeConverter.class)
    @Column(name = "owner_type")
    private WalletOwnerType ownerType;

    @Column(name = "owner_uuid")
    private UUID ownerUuid;

    @Convert(converter = PayoutMethodTypeConverter.class)
    @Column(name = "method_type")
    private PayoutMethodType methodType;

    @Column(name = "msisdn")
    private String msisdn;

    @Column(name = "label")
    private String label;

    @Convert(converter = PayoutMethodStatusConverter.class)
    @Column(name = "status")
    private PayoutMethodStatus status;

    @Column(name = "is_default")
    private boolean defaultMethod;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "unlock_at")
    private OffsetDateTime unlockAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "mpesa_registered_name")
    private String mpesaRegisteredName;

    @Column(name = "added_by_user_uuid")
    private UUID addedByUserUuid;

    /** Verified and past its hold: money may be sent to it now. */
    public boolean isUsable(OffsetDateTime now) {
        return status == PayoutMethodStatus.VERIFIED && (unlockAt == null || !unlockAt.isAfter(now));
    }
}
