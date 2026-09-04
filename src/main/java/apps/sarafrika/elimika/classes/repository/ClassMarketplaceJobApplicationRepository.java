package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
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

    Optional<ClassMarketplaceJobApplication> findByJobUuidAndUuid(UUID jobUuid, UUID uuid);

    Optional<ClassMarketplaceJobApplication> findByJobUuidAndInstructorUuid(UUID jobUuid, UUID instructorUuid);

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
}
