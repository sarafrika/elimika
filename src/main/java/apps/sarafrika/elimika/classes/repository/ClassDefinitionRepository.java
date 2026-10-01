package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.projection.TrainerClassCount;
import org.springframework.data.domain.Pageable;
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
public interface ClassDefinitionRepository extends JpaRepository<ClassDefinition, Long>,
        JpaSpecificationExecutor<ClassDefinition> {

    Optional<ClassDefinition> findByUuid(UUID uuid);

    List<ClassDefinition> findByUuidIn(Collection<UUID> uuids);

    /** Keyset page for search index rebuilds: rows after {@code id}, in id order. */
    List<ClassDefinition> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    boolean existsByUuid(UUID uuid);

    /** Every course a class is linked to, for resolving content approval in one batch. */
    @Query("SELECT DISTINCT cd.courseUuid FROM ClassDefinition cd WHERE cd.courseUuid IS NOT NULL")
    List<UUID> findDistinctCourseUuids();

    /** Every program a class is linked to, for resolving content approval in one batch. */
    @Query("SELECT DISTINCT cd.programUuid FROM ClassDefinition cd WHERE cd.programUuid IS NOT NULL")
    List<UUID> findDistinctProgramUuids();

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

    /**
     * Active class counts per course in the given visibility, grouped in one query. The grouping key
     * travels in {@link TrainerClassCount#trainerUuid()} - here it is the course.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.classes.repository.projection.TrainerClassCount(
                       cd.courseUuid, COUNT(cd))
            FROM ClassDefinition cd
            WHERE cd.courseUuid IN :courseUuids
              AND cd.isActive = true
              AND cd.classVisibility = :visibility
            GROUP BY cd.courseUuid
            """)
    List<TrainerClassCount> countActiveByCourseAndVisibility(@Param("courseUuids") Collection<UUID> courseUuids,
                                                             @Param("visibility") apps.sarafrika.elimika.shared.enums.ClassVisibility visibility);

    /**
     * A course's classes that a visitor can still join: active, in the given visibility, and neither
     * the registration window nor the teaching period ended before {@code today}. A missing end date
     * counts as open.
     */
    @Query("""
            SELECT cd
            FROM ClassDefinition cd
            WHERE cd.courseUuid = :courseUuid
              AND cd.isActive = true
              AND cd.classVisibility = :visibility
              AND (cd.registrationPeriodEndDate IS NULL OR cd.registrationPeriodEndDate >= :today)
              AND (cd.academicPeriodEndDate IS NULL OR cd.academicPeriodEndDate >= :today)
            """)
    List<ClassDefinition> findOpenByCourse(@Param("courseUuid") UUID courseUuid,
                                           @Param("visibility") apps.sarafrika.elimika.shared.enums.ClassVisibility visibility,
                                           @Param("today") java.time.LocalDate today);

    /**
     * {@link #findOpenByCourse} counted and priced per course for many courses, grouped in one query.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.classes.repository.projection.CourseOpenClassAggregate(
                       cd.courseUuid, COUNT(cd), MIN(cd.salePrice))
            FROM ClassDefinition cd
            WHERE cd.courseUuid IN :courseUuids
              AND cd.isActive = true
              AND cd.classVisibility = :visibility
              AND (cd.registrationPeriodEndDate IS NULL OR cd.registrationPeriodEndDate >= :today)
              AND (cd.academicPeriodEndDate IS NULL OR cd.academicPeriodEndDate >= :today)
            GROUP BY cd.courseUuid
            """)
    List<apps.sarafrika.elimika.classes.repository.projection.CourseOpenClassAggregate> summariseOpenByCourse(
            @Param("courseUuids") Collection<UUID> courseUuids,
            @Param("visibility") apps.sarafrika.elimika.shared.enums.ClassVisibility visibility,
            @Param("today") java.time.LocalDate today);
}
