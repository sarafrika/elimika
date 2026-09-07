package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.projection.TrainerClassCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassDefinitionRepository extends JpaRepository<ClassDefinition, Long> {

    Optional<ClassDefinition> findByUuid(UUID uuid);

    List<ClassDefinition> findByUuidIn(Collection<UUID> uuids);

    boolean existsByUuid(UUID uuid);

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

    /**
     * Seating rows for every class delivering a course, as
     * {@code [uuid (UUID), is_active (Boolean), max_participants (Integer)]}.
     * <p>
     * A projection rather than the entities: the course statistics endpoint needs identifiers and a
     * seat total, and hydrating class definitions to add up one integer column would load a course's
     * whole delivery estate on every page view.
     */
    @Query("SELECT cd.uuid, cd.isActive, cd.maxParticipants FROM ClassDefinition cd " +
           "WHERE cd.courseUuid = :courseUuid")
    List<Object[]> findClassSeatingByCourseUuid(@Param("courseUuid") UUID courseUuid);

    /**
     * The same seating rows, narrowed to the classes one instructor leads.
     */
    @Query("SELECT cd.uuid, cd.isActive, cd.maxParticipants FROM ClassDefinition cd " +
           "WHERE cd.courseUuid = :courseUuid AND cd.defaultInstructorUuid = :instructorUuid")
    List<Object[]> findClassSeatingByCourseUuidAndInstructorUuid(@Param("courseUuid") UUID courseUuid,
                                                                 @Param("instructorUuid") UUID instructorUuid);

    /**
     * The same seating rows, narrowed to the classes the given organisations own.
     */
    @Query("SELECT cd.uuid, cd.isActive, cd.maxParticipants FROM ClassDefinition cd " +
           "WHERE cd.courseUuid = :courseUuid AND cd.organisationUuid IN :organisationUuids")
    List<Object[]> findClassSeatingByCourseUuidAndOrganisationUuidIn(
            @Param("courseUuid") UUID courseUuid,
            @Param("organisationUuids") Collection<UUID> organisationUuids);

    /**
     * Active class counts on one course, grouped by the instructor delivering them.
     * <p>
     * Aggregated in the database because the caller wants a number per trainer, not the classes:
     * a class definition carries the sale price, the instructor pay and the meeting link, and none
     * of that has any business being loaded to compute a count.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.classes.repository.projection.TrainerClassCount(
                       cd.defaultInstructorUuid, COUNT(cd))
            FROM ClassDefinition cd
            WHERE cd.courseUuid = :courseUuid
              AND cd.isActive = true
              AND cd.defaultInstructorUuid IN :instructorUuids
            GROUP BY cd.defaultInstructorUuid
            """)
    List<TrainerClassCount> countActiveByCourseAndInstructor(@Param("courseUuid") UUID courseUuid,
                                                             @Param("instructorUuids") Collection<UUID> instructorUuids);

    /**
     * Active class counts on one course, grouped by the organisation running them.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.classes.repository.projection.TrainerClassCount(
                       cd.organisationUuid, COUNT(cd))
            FROM ClassDefinition cd
            WHERE cd.courseUuid = :courseUuid
              AND cd.isActive = true
              AND cd.organisationUuid IN :organisationUuids
            GROUP BY cd.organisationUuid
            """)
    List<TrainerClassCount> countActiveByCourseAndOrganisation(@Param("courseUuid") UUID courseUuid,
                                                               @Param("organisationUuids") Collection<UUID> organisationUuids);
}
