package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.SkillsFundSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillsFundSourceRepository extends JpaRepository<SkillsFundSource, Long> {

    List<SkillsFundSource> findByOrganisationUuidOrderByNameAsc(UUID organisationUuid);

    Optional<SkillsFundSource> findByUuid(UUID uuid);
}
