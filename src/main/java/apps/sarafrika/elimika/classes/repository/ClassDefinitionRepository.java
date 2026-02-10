package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassDefinitionRepository extends JpaRepository<ClassDefinition, Long> {

    Optional<ClassDefinition> findByUuid(UUID uuid);

    List<ClassDefinition> findByCourseUuid(UUID courseUuid);

    List<ClassDefinition> findByProgramUuid(UUID programUuid);

    List<ClassDefinition> findByDefaultInstructorUuid(UUID instructorUuid);

    List<ClassDefinition> findByOrganisationUuid(UUID organisationUuid);

    List<ClassDefinition> findByIsActiveTrue();

    @Query("SELECT cd FROM ClassDefinition cd WHERE cd.courseUuid = :courseUuid AND cd.isActive = true")
    List<ClassDefinition> findActiveClassesForCourse(@Param("courseUuid") UUID courseUuid);

    @Query("SELECT cd FROM ClassDefinition cd WHERE cd.programUuid = :programUuid AND cd.isActive = true")
    List<ClassDefinition> findActiveClassesForProgram(@Param("programUuid") UUID programUuid);

    @Query("SELECT cd FROM ClassDefinition cd WHERE cd.defaultInstructorUuid = :instructorUuid AND cd.isActive = true")
    List<ClassDefinition> findActiveClassesForInstructor(@Param("instructorUuid") UUID instructorUuid);
}
