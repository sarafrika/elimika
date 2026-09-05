package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.repository.projection.OrganisationTownView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository extends JpaRepository<Organisation, Long>, JpaSpecificationExecutor<Organisation> {
    Optional<Organisation> findByUuid(UUID uuid);

    List<Organisation> findByUuidIn(Collection<UUID> uuids);


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

    /**
     * The towns several organisations give as their base, without loading the rest of the row.
     * <p>
     * A projection because the caller is a public-facing directory: the entity carries coordinates,
     * a licence number and a verification state that such a caller has no claim on, and the cheapest
     * way not to disclose them is not to select them.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.tenancy.repository.projection.OrganisationTownView(
                       organisation.uuid, organisation.location)
            FROM Organisation organisation
            WHERE organisation.uuid IN :uuids
            """)
    List<OrganisationTownView> findTownsByUuidIn(@Param("uuids") Collection<UUID> uuids);
}
