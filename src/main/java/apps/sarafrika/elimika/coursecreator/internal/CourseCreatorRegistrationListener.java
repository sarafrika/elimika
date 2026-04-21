package apps.sarafrika.elimika.coursecreator.internal;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorRepository;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainRemovedEvent;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class CourseCreatorRegistrationListener {

    private final CourseCreatorRepository courseCreatorRepository;

    @ApplicationModuleListener
    void onUserDomainAssigned(UserDomainMappingEvent event) {
        if (!UserDomain.course_creator.name().equalsIgnoreCase(event.userDomain())) {
            return;
        }

        if (courseCreatorRepository.existsByUserUuid(event.userUuid())) {
            return;
        }

        CourseCreator courseCreator = new CourseCreator();
        courseCreator.setUserUuid(event.userUuid());
        courseCreator.setFullName("Unknown");
        courseCreator.setAdminVerified(false);
        courseCreatorRepository.save(courseCreator);
    }

    @ApplicationModuleListener
    void onUserDomainRemoved(UserDomainRemovedEvent event) {
        if (!UserDomain.course_creator.name().equalsIgnoreCase(event.userDomain())) {
            return;
        }

        courseCreatorRepository.findByUserUuid(event.userUuid())
                .ifPresent(courseCreatorRepository::delete);
    }
}
