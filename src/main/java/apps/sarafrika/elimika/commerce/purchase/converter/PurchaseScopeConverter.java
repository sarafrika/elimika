package apps.sarafrika.elimika.commerce.purchase.converter;

import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PurchaseScopeConverter implements AttributeConverter<PurchaseScope, String> {

    @Override
    public String convertToDatabaseColumn(PurchaseScope scope) {
        return scope != null ? scope.name() : null;
    }

    @Override
    public PurchaseScope convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return PurchaseScope.valueOf(dbData);
    }
}
