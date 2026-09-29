package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.user.UserUpdateEvent;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Re-indexes the classes an instructor teaches after their user account is updated, so the
 * {@code instructor_name} copied into class documents follows a rename the same day.
 * <p>
 * The only cross-module rename with a direct event: organisation, branch, course and program renames
 * raise none, and are refreshed by the nightly full rebuild instead.
 */
@Component
@RequiredArgsConstructor
class ClassInstructorNameChangeIndexer {

    private final InstructorLookupService instructorLookupService;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final SearchIndexRequests searchIndexRequests;

    @ApplicationModuleListener
    void onUserUpdated(UserUpdateEvent event) {
        if (!searchIndexRequests.isEnabled() || event.sarafrikaCorrelationId() == null) {
            return;
        }
        instructorLookupService.findInstructorUuidByUserUuid(event.sarafrikaCorrelationId()).ifPresent(instructorUuid -> {
            List<UUID> classes = classDefinitionRepository.findByDefaultInstructorUuid(instructorUuid).stream()
                    .map(ClassDefinition::getUuid)
                    .toList();
            searchIndexRequests.enqueue(ClassSearchSource.INDEX, classes);
        });
    }
}
