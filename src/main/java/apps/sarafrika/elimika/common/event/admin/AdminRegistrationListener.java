package apps.sarafrika.elimika.common.event.admin;

import apps.sarafrika.elimika.common.enums.UserDomain;
import apps.sarafrika.elimika.common.event.user.UserDomainMappingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for admin registration events.
 * 
 * Handles the registration of system administrators by ensuring proper domain assignment.
 * System admins have platform-wide administrative privileges.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class AdminRegistrationListener {
    
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Processes admin registration events.
     * 
     * When a user is registered as an admin, this listener ensures they are assigned
     * the admin domain through the UserDomainMappingEvent.
     *
     * @param event the admin registration event containing user details
     */
    @ApplicationModuleListener
    void onAdminRegistration(RegisterAdmin event) {
        log.info("Processing admin registration event: name={}, userUuid={}", 
                event.fullName(), event.userUuid());
        
        try {
            // Publish domain mapping event to assign admin domain
            applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(event.userUuid(), UserDomain.admin.name())
            );
            
            log.info("Successfully processed admin registration for user: {} ({})", 
                    event.fullName(), event.userUuid());
            
        } catch (Exception e) {
            log.error("Failed to process admin registration for user: {} ({})", 
                    event.fullName(), event.userUuid(), e);
            throw new RuntimeException("Admin registration failed: " + e.getMessage(), e);
        }
    }
}