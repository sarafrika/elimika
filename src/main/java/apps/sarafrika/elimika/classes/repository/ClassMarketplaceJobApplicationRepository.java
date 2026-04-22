package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplication;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
