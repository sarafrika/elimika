package apps.sarafrika.elimika.shared.currency.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "currencies")
public class PlatformCurrency extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 3)
    private String code;

    @Column(name = "numeric_code")
    private Integer numericCode;

    @Column(name = "currency_name", nullable = false, length = 128)
    private String currencyName;

    @Column(name = "symbol", length = 16)
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "is_default", nullable = false)
    private Boolean defaultCurrency;

    public PlatformCurrency(String code,
                            Integer numericCode,
                            String currencyName,
                            String symbol,
                            Integer decimalPlaces,
                            Boolean active,
                            Boolean defaultCurrency) {
        this.code = code;
        this.numericCode = numericCode;
        this.currencyName = currencyName;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.active = active;
        this.defaultCurrency = defaultCurrency;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
        if (active == null) {
            active = Boolean.TRUE;
        }
        if (defaultCurrency == null) {
            defaultCurrency = Boolean.FALSE;
        }
    }
}
