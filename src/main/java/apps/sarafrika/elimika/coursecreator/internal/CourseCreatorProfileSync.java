package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileChangedEvent;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Keeps the course creator row's copy of the shared basics in step, inside the writing transaction. */
@Component
@RequiredArgsConstructor
class CourseCreatorProfileSync {

    private final CourseCreatorRepository courseCreatorRepository;

    @EventListener
    void on(ProfessionalProfileChangedEvent event) {
        if (event.section() != ProfileSection.BASICS || event.userUuid() == null || event.basics() == null) {
            return;
        }
        courseCreatorRepository.findByUserUuid(event.userUuid())
                .filter(creator -> !CourseCreatorBasics.matches(creator, event.basics()))
                .ifPresent(creator -> {
                    CourseCreatorBasics.copy(event.basics(), creator);
                    courseCreatorRepository.save(creator);
                });
    }
}
