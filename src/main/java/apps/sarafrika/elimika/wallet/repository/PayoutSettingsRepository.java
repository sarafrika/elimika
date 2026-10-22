package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.PayoutSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutSettingsRepository extends JpaRepository<PayoutSettings, Short> {
}
