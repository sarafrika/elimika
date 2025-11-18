package apps.sarafrika.elimika.shared.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module lookup service exposing read-only class definition attributes.
 */
public interface ClassDefinitionLookupService {

    Optional<ClassDefinitionSnapshot> findByUuid(UUID classDefinitionUuid);

    default Optional<ClassDefinitionSnapshot> findByUuidWithoutCourse(UUID classDefinitionUuid) {
        return findByUuid(classDefinitionUuid).map(snapshot ->
                new ClassDefinitionSnapshot(snapshot.classDefinitionUuid(), null, snapshot.title(), snapshot.trainingFee()));
    }

    record ClassDefinitionSnapshot(
            UUID classDefinitionUuid,
            UUID courseUuid,
            String title,
            java.math.BigDecimal trainingFee
    ) { }
}
