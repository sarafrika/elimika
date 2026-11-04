package apps.sarafrika.elimika.shared.sms.template;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsTemplateRepository extends JpaRepository<SmsTemplate, Long> {

    Optional<SmsTemplate> findByTemplateKey(String templateKey);
}
