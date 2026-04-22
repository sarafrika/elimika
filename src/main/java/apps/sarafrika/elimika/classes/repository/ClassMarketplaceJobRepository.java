package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClassMarketplaceJobRepository extends JpaRepository<ClassMarketplaceJob, Long> {

    Optional<ClassMarketplaceJob> findByUuid(UUID uuid);

    @Query("""
            SELECT job FROM ClassMarketplaceJob job
            WHERE (:organisationUuid IS NULL OR job.organisationUuid = :organisationUuid)
              AND (:courseUuid IS NULL OR job.courseUuid = :courseUuid)
              AND (:status IS NULL OR job.status = :status)
            ORDER BY job.createdDate DESC
            """)
    Page<ClassMarketplaceJob> search(@Param("organisationUuid") UUID organisationUuid,
                                     @Param("courseUuid") UUID courseUuid,
                                     @Param("status") ClassMarketplaceJobStatus status,
                                     Pageable pageable);
}
