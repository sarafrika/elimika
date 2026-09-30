package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClassMarketplaceJobRequiredSkillRepository extends JpaRepository<ClassMarketplaceJobRequiredSkill, Long> {

    List<ClassMarketplaceJobRequiredSkill> findByJobUuidOrderByIdAsc(UUID jobUuid);

    List<ClassMarketplaceJobRequiredSkill> findByJobUuidInOrderByIdAsc(Collection<UUID> jobUuids);

    /** The jobs posted for a course, whose inherited skills follow the course's tags. */
    @Query("SELECT job.uuid FROM ClassMarketplaceJob job WHERE job.courseUuid = :courseUuid")
    List<UUID> findJobUuidsByCourseUuid(@Param("courseUuid") UUID courseUuid);
}
