package apps.sarafrika.elimika.shared.currency.repository;

import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformCurrencyRepository extends JpaRepository<PlatformCurrency, Long> {

    Optional<PlatformCurrency> findByCodeIgnoreCase(String code);

    Optional<PlatformCurrency> findByDefaultCurrencyTrue();

    List<PlatformCurrency> findByActiveTrueOrderByCurrencyNameAsc();
}
