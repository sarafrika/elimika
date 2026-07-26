package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    List<Competition> findByOrganisationUuidOrderByEventDateAsc(UUID organisationUuid);

    Optional<Competition> findByUuid(UUID uuid);
}
