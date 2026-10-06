package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.AccountRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRegistrationRepository extends JpaRepository<AccountRegistration, Long> {

    Optional<AccountRegistration> findFirstByUserUuidOrderByCreatedDateDesc(UUID userUuid);
}
