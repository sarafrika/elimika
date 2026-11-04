package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import apps.sarafrika.elimika.wallet.util.converter.WalletPurseConverter;
import apps.sarafrika.elimika.wallet.util.converter.WalletOwnerTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_wallets")
@Getter
@Setter
@NoArgsConstructor
public class UserWallet extends BaseEntity {

    @Convert(converter = WalletOwnerTypeConverter.class)
    @Column(name = "owner_type")
    private WalletOwnerType ownerType = WalletOwnerType.USER;

    /** The owner's uuid, user or organisation; the column keeps its historical name. */
    @Column(name = "user_uuid")
    private UUID ownerUuid;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "balance_amount")
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    /** Which of the owner's purses this row holds. */
    @Convert(converter = WalletPurseConverter.class)
    @Column(name = "purse")
    private WalletPurse purse;

    public WalletOwner owner() {
        return new WalletOwner(ownerType, ownerUuid);
    }
}
