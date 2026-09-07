package apps.sarafrika.elimika.shared.spi;

import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module lookup service exposing read-only class definition attributes.
 */
public interface ClassDefinitionLookupService {

    Optional<ClassDefinitionSnapshot> findByUuid(UUID classDefinitionUuid);

    /**
     * Resolves the default instructor UUID configured on a class definition.
     *
     * @param classDefinitionUuid the class definition UUID
     * @return the default instructor UUID, or empty when the class is missing or has no instructor
     */
    Optional<UUID> findDefaultInstructorUuid(UUID classDefinitionUuid);

    /**
     * Resolves the organisation that owns a class definition.
     * <p>
     * Needed by the module that records what an organisation owes an instructor: the obligation is
     * caused by a session, but the debtor is the organisation behind the class.
     *
     * @param classDefinitionUuid the class definition UUID
     * @return the owning organisation UUID, or empty when the class is missing or is not org-owned
     */
    Optional<UUID> findOrganisationUuid(UUID classDefinitionUuid);

    /**
     * The training branch (location) a class is delivered at.
     *
     * @param classDefinitionUuid the class definition UUID
     * @return the branch UUID, or empty when the class is missing or not tied to a branch
     */
    Optional<UUID> findBranchUuid(UUID classDefinitionUuid);

    /**
     * Resolves the owning organisation of several class definitions in one query.
     * <p>
     * An instructor's schedule can span many sessions of a handful of classes, so resolving the
     * organisation behind each session one row at a time would be a query per session.
     *
     * @param classDefinitionUuids candidate class definition UUIDs; nulls are ignored
     * @return owning organisation UUID keyed by class definition UUID, omitting classes that are
     *         missing or not organisation-owned
     */
    Map<UUID, UUID> findOrganisationUuids(Collection<UUID> classDefinitionUuids);

    /**
     * The classes delivering a course, reduced to exactly what a statistics reader needs.
     * <p>
     * Returns identifiers and an aggregate seat capacity — never per-class seat counts. Seats filled
     * beside seats offered, next to a published price, turns a course page into a revenue
     * calculator, so the capacity is summed here and only ever leaves as a ratio.
     *
     * @param courseUuid the course whose classes are wanted
     * @return the scope, empty when the course has no classes
     */
    CourseClassScope findClassScopeForCourse(UUID courseUuid);

    /**
     * The subset of {@link #findClassScopeForCourse(UUID)} delivered by one trainer: classes the
     * instructor leads, plus classes owned by any of the given organisations. Passing no instructor
     * and no organisation yields an empty scope rather than everybody's classes.
     *
     * @param courseUuid        the course whose classes are wanted
     * @param instructorUuid    the instructor whose classes count, or null
     * @param organisationUuids the organisations whose classes count; may be empty
     * @return the trainer's scope, empty when they deliver none of the course
     */
    CourseClassScope findClassScopeForCourseAndTrainer(UUID courseUuid,
                                                       UUID instructorUuid,
                                                       Collection<UUID> organisationUuids);

    List<UUID> findClassDefinitionUuidsByInstructorUuid(UUID instructorUuid);

    List<UUID> findClassDefinitionUuidsByOrganisationUuid(UUID organisationUuid);

    /**
     * How many active classes each of these instructors is running on one course.
     * <p>
     * Counted in the database and grouped in one query: a course's trainer directory asks this of
     * every row at once, and answering it by fetching each instructor's classes and sizing the list
     * would load whole class definitions — titles, prices, meeting links — to produce an integer.
     *
     * @param courseUuid      the course the classes must belong to
     * @param instructorUuids candidate instructor identifiers; nulls are ignored
     * @return active class count keyed by instructor UUID, omitting instructors running none
     */
    Map<UUID, Long> countActiveCourseClassesByInstructor(UUID courseUuid, Collection<UUID> instructorUuids);

    /**
     * How many active classes each of these organisations is running on one course. The organisation
     * counterpart of {@link #countActiveCourseClassesByInstructor(UUID, Collection)}.
     *
     * @param courseUuid        the course the classes must belong to
     * @param organisationUuids candidate organisation identifiers; nulls are ignored
     * @return active class count keyed by organisation UUID, omitting organisations running none
     */
    Map<UUID, Long> countActiveCourseClassesByOrganisation(UUID courseUuid, Collection<UUID> organisationUuids);

    default Optional<ClassDefinitionSnapshot> findByUuidWithoutCourse(UUID classDefinitionUuid) {
        return findByUuid(classDefinitionUuid).map(snapshot ->
                new ClassDefinitionSnapshot(
                        snapshot.classDefinitionUuid(),
                        null,
                        snapshot.programUuid(),
                        snapshot.title(),
                        snapshot.description(),
                        snapshot.salePrice(),
                        snapshot.instructorPay(),
                        snapshot.rateBasis(),
                        snapshot.classVisibility(),
                        snapshot.locationType(),
                        snapshot.maxParticipants(),
                        snapshot.allowWaitlist(),
                        snapshot.classReminderMinutes(),
                        snapshot.registrationOpensOn(),
                        snapshot.registrationClosesOn()));
    }

    /**
     * The classes delivering a course.
     *
     * @param allClassUuids       every class definition for the course, running or not
     * @param activeClassUuids    the subset still active
     * @param activeSeatCapacity  seats offered across the active classes. Deliberately an aggregate:
     *                            callers publish a fill percentage, never the seat counts behind it.
     */
    record CourseClassScope(
            List<UUID> allClassUuids,
            List<UUID> activeClassUuids,
            long activeSeatCapacity
    ) {
        public static CourseClassScope empty() {
            return new CourseClassScope(List.of(), List.of(), 0L);
        }
    }

    record ClassDefinitionSnapshot(
            UUID classDefinitionUuid,
            UUID courseUuid,
            UUID programUuid,
            String title,
            String description,
            java.math.BigDecimal salePrice,
            java.math.BigDecimal instructorPay,
            apps.sarafrika.elimika.shared.utils.enums.RateBasis rateBasis,
            ClassVisibility classVisibility,
            LocationType locationType,
            Integer maxParticipants,
            Boolean allowWaitlist,
            Integer classReminderMinutes,

            /** First day, inclusive, on which this class accepts enrolments. */
            LocalDate registrationOpensOn,

            /** Last day, inclusive, on which this class accepts enrolments. */
            LocalDate registrationClosesOn
    ) { }
}
