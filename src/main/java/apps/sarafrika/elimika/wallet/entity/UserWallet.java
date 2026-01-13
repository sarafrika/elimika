package apps.sarafrika.elimika.wallet.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
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

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "balance_amount")
    private BigDecimal balanceAmount = BigDecimal.ZERO;
}
