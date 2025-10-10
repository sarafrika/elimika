package apps.sarafrika.elimika.shared.tracking.repository;

import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestAuditLogRepository extends JpaRepository<RequestAuditLog, Long> {
}
