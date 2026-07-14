package apps.sarafrika.elimika.resourcing.repository;

import apps.sarafrika.elimika.resourcing.model.OrganisationResource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationResourceRepository extends JpaRepository<OrganisationResource, Long>,
        JpaSpecificationExecutor<OrganisationResource> {

    Optional<OrganisationResource> findByUuid(UUID uuid);

    List<OrganisationResource> findByUuidIn(Collection<UUID> uuids);

    boolean existsByOrganisationUuidAndNameIgnoreCase(UUID organisationUuid, String name);

    /**
     * Locks the resource rows so concurrent booking attempts on the same resources
     * serialise. Ordered by id so competing transactions acquire locks in the same
     * order and cannot deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OrganisationResource r WHERE r.uuid IN :uuids ORDER BY r.id")
    List<OrganisationResource> lockByUuids(@Param("uuids") Collection<UUID> uuids);
}
