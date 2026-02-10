package apps.sarafrika.elimika.shared.spi;

import java.util.Optional;
import java.util.UUID;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;

/**
 * Cross-module lookup service exposing read-only class definition attributes.
 */
public interface ClassDefinitionLookupService {

    Optional<ClassDefinitionSnapshot> findByUuid(UUID classDefinitionUuid);

    default Optional<ClassDefinitionSnapshot> findByUuidWithoutCourse(UUID classDefinitionUuid) {
        return findByUuid(classDefinitionUuid).map(snapshot ->
                new ClassDefinitionSnapshot(
                        snapshot.classDefinitionUuid(),
                        null,
                        snapshot.programUuid(),
                        snapshot.title(),
                        snapshot.description(),
                        snapshot.trainingFee(),
                        snapshot.classVisibility(),
                        snapshot.maxParticipants(),
                        snapshot.allowWaitlist()));
    }

    record ClassDefinitionSnapshot(
            UUID classDefinitionUuid,
            UUID courseUuid,
            UUID programUuid,
            String title,
            String description,
            java.math.BigDecimal trainingFee,
            ClassVisibility classVisibility,
            Integer maxParticipants,
            Boolean allowWaitlist
    ) { }
}
