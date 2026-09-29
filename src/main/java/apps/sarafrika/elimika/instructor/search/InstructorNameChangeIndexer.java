package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.event.user.UserUpdateEvent;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Re-indexes an instructor after their user account is updated.
 * <p>
 * {@code instructors.full_name} is maintained by a database trigger on {@code users}, so a name change
 * never passes through an {@code Instructor} entity and no search trigger fires for it. The user
 * update event is the only signal this module receives.
 */
@Component
@RequiredArgsConstructor
class InstructorNameChangeIndexer {

    private final InstructorRepository instructorRepository;
    private final SearchIndexRequests searchIndexRequests;

    @ApplicationModuleListener
    void onUserUpdated(UserUpdateEvent event) {
        if (!searchIndexRequests.isEnabled() || event.sarafrikaCorrelationId() == null) {
            return;
        }
        instructorRepository.findByUserUuid(event.sarafrikaCorrelationId())
                .ifPresent(instructor -> searchIndexRequests.enqueue(InstructorSearchSource.INDEX, instructor.getUuid()));
    }
}
