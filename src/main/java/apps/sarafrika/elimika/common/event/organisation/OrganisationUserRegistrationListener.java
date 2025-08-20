package apps.sarafrika.elimika.common.event.organisation;

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
 * Listener for organisation user registration events.
 * 
 * Handles the registration of organisation users by ensuring proper domain assignment.
 * Organisation users can be:
 * 1. Organisation Administrators - Full organizational control
 * 2. Branch-Level Users - Limited to specific training branches
 * 
 * The distinction is managed through UserOrganisationDomainMapping configurations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED)
public class OrganisationUserRegistrationListener {
    
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Processes organisation user registration events.
     * 
     * When a user is registered as an organisation_user, this listener ensures they are assigned
     * the organisation_user domain through the UserDomainMappingEvent.
     * 
     * The specific permissions (org-wide vs branch-specific) are handled separately
     * through the UserOrganisationDomainMapping system.
     *
     * @param event the organisation user registration event containing user and org details
     */
    @ApplicationModuleListener
    void onOrganisationUserRegistration(RegisterOrganisationUser event) {
        log.info("Processing organisation user registration event: name={}, userUuid={}, organisationUuid={}, isOrgAdmin={}", 
                event.fullName(), event.userUuid(), event.organisationUuid(), event.isOrganisationAdmin());
        
        try {
            // Publish domain mapping event to assign organisation_user domain
            applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(event.userUuid(), UserDomain.organisation_user.name())
            );
            
            String userType = event.isOrganisationAdmin() ? "organisation administrator" : "branch-level user";
            log.info("Successfully processed organisation user registration: {} ({}) as {} in organisation {}", 
                    event.fullName(), event.userUuid(), userType, event.organisationUuid());
            
        } catch (Exception e) {
            log.error("Failed to process organisation user registration for user: {} ({}) in organisation {}", 
                    event.fullName(), event.userUuid(), event.organisationUuid(), e);
            throw new RuntimeException("Organisation user registration failed: " + e.getMessage(), e);
        }
    }
}