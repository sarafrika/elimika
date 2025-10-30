package apps.sarafrika.elimika.shared.currency.service;

import apps.sarafrika.elimika.shared.currency.dto.CurrencyCreateRequest;
import apps.sarafrika.elimika.shared.currency.dto.CurrencyDTO;
import apps.sarafrika.elimika.shared.currency.dto.CurrencyUpdateRequest;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;

import java.util.List;

public interface CurrencyService {

    List<CurrencyDTO> getActiveCurrencies();

    List<CurrencyDTO> getAllCurrencies();

    CurrencyDTO getDefaultCurrency();

    CurrencyDTO createCurrency(CurrencyCreateRequest request);

    CurrencyDTO updateCurrency(String code, CurrencyUpdateRequest request);

    CurrencyDTO setDefaultCurrency(String code);

    CurrencyDTO toggleCurrency(String code, boolean active);

    PlatformCurrency resolveCurrencyOrDefault(String code);
}
