package apps.sarafrika.elimika.shared.audit;

import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

public class ApplicationAuditAware implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of("elimika");
    }
}
