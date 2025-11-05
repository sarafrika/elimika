package apps.sarafrika.elimika.shared.tracking.repository;

import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestAuditLogRepository extends JpaRepository<RequestAuditLog, Long> {

    @Query("""
            SELECT log FROM RequestAuditLog log
            WHERE LOWER(log.requestUri) LIKE LOWER(CONCAT(:prefix, '%'))
              AND (log.responseStatus IS NULL OR (log.responseStatus >= :statusStart AND log.responseStatus <= :statusEnd))
            """)
    Page<RequestAuditLog> findAdminActivity(@Param("prefix") String prefix,
                                            @Param("statusStart") int statusStart,
                                            @Param("statusEnd") int statusEnd,
                                            Pageable pageable);
}
