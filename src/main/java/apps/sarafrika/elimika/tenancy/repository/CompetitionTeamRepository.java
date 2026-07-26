package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.CompetitionTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetitionTeamRepository extends JpaRepository<CompetitionTeam, Long> {

    List<CompetitionTeam> findByCompetitionUuid(UUID competitionUuid);

    void deleteByCompetitionUuidAndUuid(UUID competitionUuid, UUID uuid);

    /** Team counts for a set of competitions, as [competitionUuid, count] rows. */
    @Query("SELECT t.competitionUuid, COUNT(t) FROM CompetitionTeam t WHERE t.competitionUuid IN :competitionUuids GROUP BY t.competitionUuid")
    List<Object[]> countByCompetitionUuids(@Param("competitionUuids") List<UUID> competitionUuids);
}
