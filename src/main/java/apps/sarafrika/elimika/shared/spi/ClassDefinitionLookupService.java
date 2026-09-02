package apps.sarafrika.elimika.shared.spi;

import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;

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

    List<UUID> findClassDefinitionUuidsByInstructorUuid(UUID instructorUuid);

    List<UUID> findClassDefinitionUuidsByOrganisationUuid(UUID organisationUuid);

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
                        snapshot.classReminderMinutes()));
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
            Integer classReminderMinutes
    ) { }
}
