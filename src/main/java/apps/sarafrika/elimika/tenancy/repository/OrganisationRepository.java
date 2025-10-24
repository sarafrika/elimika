package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.Organisation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, Long>, JpaSpecificationExecutor<Organisation> {
    Optional<Organisation> findByUuid(UUID uuid);


    Optional<Organisation> findByName(String name);


    boolean existsBySlug(String slug);

    Page<Organisation> findByDeletedFalse(Pageable pageable);

    Optional<Organisation> findByUuidAndDeletedFalse(UUID uuid);

    Page<Organisation> findByAdminVerifiedTrueAndDeletedFalse(Pageable pageable);

    @Query("SELECT o FROM Organisation o WHERE (o.adminVerified = false OR o.adminVerified IS NULL) AND o.deleted = false")
    Page<Organisation> findByAdminVerifiedFalseOrNullAndDeletedFalse(Pageable pageable);

    long countByDeletedFalse();

    long countByActiveTrueAndDeletedFalse();

    long countByActiveFalseAndDeletedFalse();

    @Query("SELECT COUNT(o) FROM Organisation o WHERE (o.adminVerified = false OR o.adminVerified IS NULL) AND o.deleted = false")
    long countPendingApproval();
}
