package apps.sarafrika.elimika.shared.currency.service.impl;

import apps.sarafrika.elimika.shared.currency.dto.CurrencyCreateRequest;
import apps.sarafrika.elimika.shared.currency.dto.CurrencyDTO;
import apps.sarafrika.elimika.shared.currency.dto.CurrencyUpdateRequest;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.repository.PlatformCurrencyRepository;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrencyServiceImpl implements CurrencyService {

    private static final String CURRENCY_NOT_FOUND_TEMPLATE = "Currency %s is not registered on the platform";

    private final PlatformCurrencyRepository repository;

    @Override
    @Transactional
    public List<CurrencyDTO> getActiveCurrencies() {
        return repository.findByActiveTrueOrderByCurrencyNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<CurrencyDTO> getAllCurrencies() {
        return repository.findAll().stream()
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .map(this::toDto)
                .toList();
    }

    @Override
    public CurrencyDTO getDefaultCurrency() {
        PlatformCurrency currency = repository.findByDefaultCurrencyTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Default currency is not configured"));
        return toDto(currency);
    }

    @Override
    public CurrencyDTO createCurrency(CurrencyCreateRequest request) {
        String normalizedCode = request.code().toUpperCase(Locale.ROOT);
        repository.findByCodeIgnoreCase(normalizedCode).ifPresent(existing -> {
            throw new IllegalStateException("Currency " + normalizedCode + " already exists");
        });

        boolean defaultRequested = Boolean.TRUE.equals(request.defaultCurrency());
        boolean active = defaultRequested;

        PlatformCurrency entity = new PlatformCurrency(
                normalizedCode,
                request.numericCode(),
                request.name(),
                request.symbol(),
                request.decimalPlaces(),
                active,
                Boolean.FALSE
        );

        PlatformCurrency saved = repository.save(entity);

        if (defaultRequested) {
            saved = setDefaultCurrencyInternal(normalizedCode);
        }

        return toDto(saved);
    }

    @Override
    public CurrencyDTO updateCurrency(String code, CurrencyUpdateRequest request) {
        PlatformCurrency currency = findCurrency(code);

        if (StringUtils.hasText(request.name())) {
            currency.setCurrencyName(request.name());
        }
        if (request.symbol() != null) {
            currency.setSymbol(request.symbol());
        }
        if (request.numericCode() != null) {
            currency.setNumericCode(request.numericCode());
        }
        if (request.decimalPlaces() != null) {
            currency.setDecimalPlaces(request.decimalPlaces());
        }
        if (request.active() != null) {
            currency.setActive(request.active());
        }

        PlatformCurrency updated = repository.save(currency);
        return toDto(updated);
    }

    @Override
    public CurrencyDTO setDefaultCurrency(String code) {
        return toDto(setDefaultCurrencyInternal(code.toUpperCase(Locale.ROOT)));
    }

    @Override
    public CurrencyDTO toggleCurrency(String code, boolean active) {
        PlatformCurrency currency = findCurrency(code);
        if (!active && Boolean.TRUE.equals(currency.getDefaultCurrency())) {
            throw new IllegalStateException("Cannot deactivate the default platform currency. Assign a new default first.");
        }
        currency.setActive(active);
        PlatformCurrency updated = repository.save(currency);
        return toDto(updated);
    }

    @Override
    @Transactional
    public PlatformCurrency resolveCurrencyOrDefault(String code) {
        if (!StringUtils.hasText(code)) {
            return repository.findByDefaultCurrencyTrue()
                    .orElseThrow(() -> new ResourceNotFoundException("Default currency is not configured"));
        }

        PlatformCurrency currency = repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CURRENCY_NOT_FOUND_TEMPLATE, code)));

        if (Boolean.FALSE.equals(currency.getActive())) {
            throw new IllegalStateException(String.format("Currency %s is inactive", currency.getCode()));
        }

        return currency;
    }

    private PlatformCurrency setDefaultCurrencyInternal(String code) {
        repository.findByDefaultCurrencyTrue().ifPresent(existing -> {
            if (!existing.getCode().equalsIgnoreCase(code)) {
                existing.setDefaultCurrency(false);
                repository.save(existing);
            }
        });

        PlatformCurrency currency = findCurrency(code);
        currency.setDefaultCurrency(true);
        currency.setActive(true);
        return repository.save(currency);
    }

    private PlatformCurrency findCurrency(String code) {
        return repository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CURRENCY_NOT_FOUND_TEMPLATE, code)));
    }

    private CurrencyDTO toDto(PlatformCurrency entity) {
        return new CurrencyDTO(
                entity.getCode(),
                entity.getCurrencyName(),
                entity.getNumericCode(),
                entity.getSymbol(),
                entity.getDecimalPlaces(),
                Boolean.TRUE.equals(entity.getActive()),
                Boolean.TRUE.equals(entity.getDefaultCurrency())
        );
    }
}
