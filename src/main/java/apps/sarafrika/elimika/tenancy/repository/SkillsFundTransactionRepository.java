package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.SkillsFundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillsFundTransactionRepository extends JpaRepository<SkillsFundTransaction, Long> {

    List<SkillsFundTransaction> findByOrganisationUuidOrderByTransactionDateDesc(UUID organisationUuid);

    Optional<SkillsFundTransaction> findByUuid(UUID uuid);
}
