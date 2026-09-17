package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.repository.projection.JobApplicationCount;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassMarketplaceJobApplicationRepository extends JpaRepository<ClassMarketplaceJobApplication, Long> {

    Optional<ClassMarketplaceJobApplication> findByUuid(UUID uuid);

    List<ClassMarketplaceJobApplication> findByUuidIn(Collection<UUID> uuids);

    /** Application counts for a page of jobs in one grouped query, leaving out one status. */
    @Query("""
            SELECT new apps.sarafrika.elimika.classes.repository.projection.JobApplicationCount(
                       application.jobUuid, COUNT(application))
            FROM ClassMarketplaceJobApplication application
            WHERE application.jobUuid IN :jobUuids
              AND application.status <> :excludedStatus
            GROUP BY application.jobUuid
            """)
    List<JobApplicationCount> countByJobUuidInExcludingStatus(
            @Param("jobUuids") Collection<UUID> jobUuids,
            @Param("excludedStatus") ClassMarketplaceJobApplicationStatus excludedStatus);

    Optional<ClassMarketplaceJobApplication> findByJobUuidAndUuid(UUID jobUuid, UUID uuid);

    Optional<ClassMarketplaceJobApplication> findByJobUuidAndInstructorUuid(UUID jobUuid, UUID instructorUuid);

    List<ClassMarketplaceJobApplication> findByInstructorUuidAndJobUuidIn(UUID instructorUuid, Collection<UUID> jobUuids);

    Page<ClassMarketplaceJobApplication> findByJobUuidOrderByCreatedDateDesc(UUID jobUuid, Pageable pageable);

    Page<ClassMarketplaceJobApplication> findByJobUuidAndStatusOrderByCreatedDateDesc(UUID jobUuid,
                                                                                      ClassMarketplaceJobApplicationStatus status,
                                                                                      Pageable pageable);

    Page<ClassMarketplaceJobApplication> findByInstructorUuidOrderByCreatedDateDesc(UUID instructorUuid, Pageable pageable);

    Page<ClassMarketplaceJobApplication> findByInstructorUuidAndStatusOrderByCreatedDateDesc(UUID instructorUuid,
                                                                                             ClassMarketplaceJobApplicationStatus status,
                                                                                             Pageable pageable);

    List<ClassMarketplaceJobApplication> findByJobUuidAndStatusIn(UUID jobUuid,
                                                                  Collection<ClassMarketplaceJobApplicationStatus> statuses);

    /**
     * Whether the instructor has applied to any advert posted by one of the given organisations.
     * <p>
     * Lets the poster of a job read the applicant's file for as long as the application exists,
     * without granting anything to an organisation the instructor never approached.
     */
    @Query("""
            SELECT COUNT(application) > 0 FROM ClassMarketplaceJobApplication application
            JOIN ClassMarketplaceJob job ON job.uuid = application.jobUuid
            WHERE application.instructorUuid = :instructorUuid
              AND job.organisationUuid IN :organisationUuids
            """)
    boolean existsByInstructorUuidAndJobOrganisationUuidIn(@Param("instructorUuid") UUID instructorUuid,
                                                           @Param("organisationUuids") Collection<UUID> organisationUuids);

    /**
     * One instructor's applications, restricted to the jobs posted by the given organisations.
     * <p>
     * The restriction is expressed in the query rather than by filtering a full by-instructor scan
     * in memory, so an organisation manager reading an instructor's history pays for one page and
     * cannot be used to force a table scan.
     */
    @Query("""
            SELECT application FROM ClassMarketplaceJobApplication application
            WHERE application.instructorUuid = :instructorUuid
              AND (:status IS NULL OR application.status = :status)
              AND application.jobUuid IN (
                    SELECT job.uuid FROM ClassMarketplaceJob job
                    WHERE job.organisationUuid IN :organisationUuids)
            ORDER BY application.createdDate DESC
            """)
    Page<ClassMarketplaceJobApplication> findByInstructorUuidAndJobOrganisations(
            @Param("instructorUuid") UUID instructorUuid,
            @Param("status") ClassMarketplaceJobApplicationStatus status,
            @Param("organisationUuids") Collection<UUID> organisationUuids,
            Pageable pageable);
}
