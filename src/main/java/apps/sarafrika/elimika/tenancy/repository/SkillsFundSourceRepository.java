package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.SkillsFundSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillsFundSourceRepository extends JpaRepository<SkillsFundSource, Long> {

    /**
     * The live sources of one fund. Soft-deleted rows are excluded here and therefore drop out of
     * both the listing and the balance, which is the whole effect of removing a source.
     */
    List<SkillsFundSource> findByOrganisationUuidAndDeletedFalseOrderByNameAsc(UUID organisationUuid);

    /**
     * Resolves a source regardless of its deleted flag. Used by the authorization guard, which has to
     * name an owning organisation even for a row that has already been removed.
     */
    Optional<SkillsFundSource> findByUuid(UUID uuid);

    Optional<SkillsFundSource> findByUuidAndDeletedFalse(UUID uuid);
}
