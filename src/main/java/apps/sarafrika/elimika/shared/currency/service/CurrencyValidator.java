package apps.sarafrika.elimika.shared.currency.service;

import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.repository.PlatformCurrencyRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CurrencyValidator {

    private final PlatformCurrencyRepository currencyRepository;

    public void validateActiveCurrency(String currencyCode) {
        String normalized = normalize(currencyCode);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("A currency_code is required for this operation");
        }

        PlatformCurrency currency = currencyRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Currency %s is not supported on this platform", normalized)
                ));

        if (!Boolean.TRUE.equals(currency.getActive())) {
            throw new IllegalArgumentException(
                    String.format("Currency %s is inactive. Choose an active platform currency.", normalized)
            );
        }
    }

    private String normalize(String code) {
        return Optional.ofNullable(code)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .orElse(null);
    }
}
