package apps.sarafrika.elimika.common.service;

import apps.sarafrika.elimika.common.event.organisation.OrganisationCreationFailureEvent;
import apps.sarafrika.elimika.common.event.organisation.SuccessfulOrganisationCreationEvent;
import apps.sarafrika.elimika.common.util.EmailUtility;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailUtility emailUtility;
    private final UserRepository userRepository;

    @ApplicationModuleListener
    void onSuccessfulOrganisationCreation(SuccessfulOrganisationCreationEvent event) {
        log.debug("Handling successful organisation creation event for user {}", event.userUuid());
        userRepository.findByUuid(event.userUuid()).ifPresent(user -> {
            try {
                emailUtility.sendOrganisationRegistrationSuccess(user.getEmail(), user.getFirstName(), event.organisationName());
            } catch (MessagingException e) {
                log.error("Failed to send success email for user {}", event.userUuid(), e);
            }
        });
    }

    @ApplicationModuleListener
    void onOrganisationCreationFailure(OrganisationCreationFailureEvent event) {
        log.debug("Handling organisation creation failure event for user {}", event.userUuid());
        userRepository.findByUuid(event.userUuid()).ifPresent(user -> {
            try {
                emailUtility.sendOrganisationRegistrationFailure(user.getEmail(), user.getFirstName(), event.organisationName(), event.errorMessage());
            } catch (MessagingException e) {
                log.error("Failed to send failure email for user {}", event.userUuid(), e);
            }
        });
    }
}
