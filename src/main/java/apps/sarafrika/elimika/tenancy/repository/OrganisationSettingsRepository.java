package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.OrganisationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationSettingsRepository extends JpaRepository<OrganisationSettings, Long> {

    Optional<OrganisationSettings> findByOrganisationUuid(UUID organisationUuid);
}
