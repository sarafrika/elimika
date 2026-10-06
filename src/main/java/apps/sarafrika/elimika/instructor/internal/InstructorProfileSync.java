package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.search.InstructorSearchSource;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileChangedEvent;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Keeps the instructor row's basics in step with the shared profile and re-indexes on skill or experience changes. */
@Component
@RequiredArgsConstructor
class InstructorProfileSync {

    private final InstructorRepository instructorRepository;
    private final SearchIndexRequests searchIndexRequests;

    @EventListener
    void on(ProfessionalProfileChangedEvent event) {
        if (event.userUuid() == null) {
            return;
        }
        switch (event.section()) {
            case BASICS -> instructorRepository.findByUserUuid(event.userUuid())
                    .ifPresent(instructor -> syncBasics(instructor, event.basics()));
            case SKILLS, EXPERIENCE -> instructorRepository.findByUserUuid(event.userUuid())
                    .ifPresent(instructor -> searchIndexRequests.enqueue(InstructorSearchSource.INDEX, instructor.getUuid()));
            default -> {
                // Other sections are not part of the instructor's search document.
            }
        }
    }

    private void syncBasics(Instructor instructor, ProfessionalProfileDTO basics) {
        if (basics == null || InstructorBasics.matches(instructor, basics)) {
            return;
        }
        InstructorBasics.copy(basics, instructor);
        instructorRepository.save(instructor);
    }
}
