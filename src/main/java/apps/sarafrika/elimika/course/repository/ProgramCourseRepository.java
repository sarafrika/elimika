package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramCourseRepository extends JpaRepository<ProgramCourse, Long>, JpaSpecificationExecutor<ProgramCourse> {
    Optional<ProgramCourse> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    List<ProgramCourse>  findByProgramUuidAndIsRequiredTrue(UUID programUuid);

    List<ProgramCourse> findByProgramUuidOrderBySequenceOrderAsc(UUID programUuid);

    List<ProgramCourse> findByProgramUuidAndIsRequiredFalse(UUID programUuid);

    long countByProgramUuid(UUID programUuid);

    long countByProgramUuidAndIsRequiredTrue(UUID programUuid);

    boolean existsByUuid(UUID uuid);

    /**
     * The courses sitting inside a set of programs. Second hop of turning a program-level training
     * approval into the courses it grants, in one query rather than one per program.
     */
    @Query("SELECT pc.courseUuid FROM ProgramCourse pc WHERE pc.programUuid IN :programUuids")
    List<UUID> findCourseUuidsByProgramUuidIn(@Param("programUuids") Collection<UUID> programUuids);
}