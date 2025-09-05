package apps.sarafrika.elimika.shared.config;

import apps.sarafrika.elimika.shared.internal.DatabaseAuditListener;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class makes sure the audit listener can access the validator and entity manager.
 * Spring will automatically run this when your application starts.
 */
@Component
@Slf4j
public class DatabaseAuditConfiguration {

    @Autowired
    public DatabaseAuditConfiguration(Validator validator, EntityManager entityManager) {
        log.info("Setting up Database Audit Configuration...");

        DatabaseAuditListener.setValidator(validator);
        DatabaseAuditListener.setEntityManager(entityManager);

        log.info("Database Audit Configuration ready!");
    }
}