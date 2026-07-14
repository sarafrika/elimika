package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassMarketplaceJobRepository extends JpaRepository<ClassMarketplaceJob, Long> {

    Optional<ClassMarketplaceJob> findByUuid(UUID uuid);

    @Query("""
            SELECT job FROM ClassMarketplaceJob job
            WHERE (:organisationUuid IS NULL OR job.organisationUuid = :organisationUuid)
              AND (:courseUuid IS NULL OR job.courseUuid = :courseUuid)
              AND (:programUuid IS NULL OR job.programUuid = :programUuid)
              AND (:status IS NULL OR job.status = :status)
            ORDER BY job.createdDate DESC
            """)
    Page<ClassMarketplaceJob> search(@Param("organisationUuid") UUID organisationUuid,
                                     @Param("courseUuid") UUID courseUuid,
                                     @Param("programUuid") UUID programUuid,
                                     @Param("status") ClassMarketplaceJobStatus status,
                                     Pageable pageable);

    /**
     * Open jobs whose recruitment window has passed: the registration period (or,
     * when none is set, the academic period) ended before the given date.
     */
    @Query("""
            SELECT job FROM ClassMarketplaceJob job
            WHERE job.status = apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus.OPEN
              AND (
                  (job.registrationPeriodEndDate IS NOT NULL AND job.registrationPeriodEndDate < :date)
                  OR (job.registrationPeriodEndDate IS NULL
                      AND job.academicPeriodEndDate IS NOT NULL
                      AND job.academicPeriodEndDate < :date)
              )
            """)
    List<ClassMarketplaceJob> findExpiredOpenJobs(@Param("date") LocalDate date);
}
