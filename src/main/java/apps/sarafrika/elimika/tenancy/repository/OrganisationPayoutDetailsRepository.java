package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.OrganisationPayoutDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationPayoutDetailsRepository extends JpaRepository<OrganisationPayoutDetails, Long> {

    Optional<OrganisationPayoutDetails> findByOrganisationUuid(UUID organisationUuid);

    boolean existsByOrganisationUuid(UUID organisationUuid);
}
