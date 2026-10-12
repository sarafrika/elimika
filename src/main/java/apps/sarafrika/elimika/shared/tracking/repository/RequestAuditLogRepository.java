package apps.sarafrika.elimika.shared.tracking.repository;

import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestAuditLogRepository extends JpaRepository<RequestAuditLog, Long>,
        JpaSpecificationExecutor<RequestAuditLog> {

    /** Admin requests in the status band; the literal prefix matches idx_request_audit_admin_activity. */
    @Query("""
            SELECT log FROM RequestAuditLog log
            WHERE LOWER(log.requestUri) LIKE '/api/v1/admin%'
              AND (log.responseStatus IS NULL OR (log.responseStatus >= :statusStart AND log.responseStatus <= :statusEnd))
            """)
    List<RequestAuditLog> findAdminActivity(@Param("statusStart") int statusStart,
                                            @Param("statusEnd") int statusEnd,
                                            Pageable pageable);

    /** Native so the predicate stays textually identical to the partial index and the count is index-only. */
    @Query(nativeQuery = true, value = """
            SELECT count(*) FROM request_audit_log
            WHERE lower(request_uri) LIKE '/api/v1/admin%'
              AND (response_status IS NULL OR (response_status >= :statusStart AND response_status <= :statusEnd))
            """)
    long countAdminActivity(@Param("statusStart") int statusStart, @Param("statusEnd") int statusEnd);
}
