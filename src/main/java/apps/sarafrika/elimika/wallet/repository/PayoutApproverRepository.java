package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.PayoutApprover;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutApproverRepository extends JpaRepository<PayoutApprover, Long> {

    List<PayoutApprover> findByOrganisationUuidOrderByIdAsc(UUID organisationUuid);

    Optional<PayoutApprover> findByOrganisationUuidAndUserUuid(UUID organisationUuid, UUID userUuid);

    boolean existsByOrganisationUuidAndUserUuid(UUID organisationUuid, UUID userUuid);
}
