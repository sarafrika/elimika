package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.wallet.enums.LedgerAccountStatus;
import apps.sarafrika.elimika.wallet.enums.LedgerAccountType;
import apps.sarafrika.elimika.wallet.enums.LedgerOwnerType;
import apps.sarafrika.elimika.wallet.enums.LedgerPurse;
import apps.sarafrika.elimika.wallet.util.converter.LedgerAccountStatusConverter;
import apps.sarafrika.elimika.wallet.util.converter.LedgerAccountTypeConverter;
import apps.sarafrika.elimika.wallet.util.converter.LedgerOwnerTypeConverter;
import apps.sarafrika.elimika.wallet.util.converter.LedgerPurseConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An account money can sit in. Identity is {@code (ownerType, ownerUuid, purse, currencyCode)}.
 */
@Entity
@Table(name = "ledger_accounts")
@Getter
@Setter
@NoArgsConstructor
public class LedgerAccount extends BaseEntity {

    @Convert(converter = LedgerOwnerTypeConverter.class)
    @Column(name = "owner_type")
    private LedgerOwnerType ownerType;

    /** Null only for the platform's own internal accounts. */
    @Column(name = "owner_uuid")
    private UUID ownerUuid;

    @Convert(converter = LedgerAccountTypeConverter.class)
    @Column(name = "account_type")
    private LedgerAccountType accountType;

    @Convert(converter = LedgerPurseConverter.class)
    @Column(name = "purse")
    private LedgerPurse purse;

    @Column(name = "currency_code")
    private String currencyCode;

    @Convert(converter = LedgerAccountStatusConverter.class)
    @Column(name = "status")
    private LedgerAccountStatus status = LedgerAccountStatus.ACTIVE;
}
