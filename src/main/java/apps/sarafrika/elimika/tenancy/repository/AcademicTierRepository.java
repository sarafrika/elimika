package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.AcademicTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicTierRepository extends JpaRepository<AcademicTier, Long> {

    /**
     * The shared platform catalogue for one education system, in curriculum order.
     * <p>
     * {@code organisationUuid IS NULL} is the whole definition of "platform row": tenant-defined
     * tiers are deliberately excluded so this reference list can be cached and shared.
     */
    List<AcademicTier> findByEducationSystemIgnoreCaseAndOrganisationUuidIsNullAndActiveTrueOrderByTierOrderAsc(
            String educationSystem);

    Optional<AcademicTier> findByUuid(UUID uuid);

    List<AcademicTier> findByUuidIn(Collection<UUID> uuids);
}
