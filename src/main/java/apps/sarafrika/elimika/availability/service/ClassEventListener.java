package apps.sarafrika.elimika.availability.service;

import apps.sarafrika.elimika.shared.event.classes.ClassDefinedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionUpdatedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionDeactivatedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for class-related events in the availability module.
 * <p>
 * This component listens to events from the Classes module and can perform
 * availability-related actions such as logging or validation.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassEventListener {

    /**
     * Handles class definition creation events.
     * Logs when a new class is created and could trigger availability validation.
     *
     * @param event The class defined event
     */
    @EventListener
    public void handleClassDefined(ClassDefinedEventDTO event) {
        log.info("Class '{}' defined for instructor {} - UUID: {}", 
                event.title(), 
                event.defaultInstructorUuid(), 
                event.definitionUuid());
                
        // Future enhancement: Could trigger automatic availability checks
        // or suggest optimal scheduling times based on instructor availability
        log.debug("Consider checking instructor {} availability for optimal class scheduling", 
                event.defaultInstructorUuid());
    }

    /**
     * Handles class definition update events.
     * Logs when a class definition is updated.
     *
     * @param event The class definition updated event
     */
    @EventListener
    public void handleClassDefinitionUpdated(ClassDefinitionUpdatedEventDTO event) {
        log.info("Class definition updated: '{}' - UUID: {}", 
                event.title(), 
                event.definitionUuid());
                
        // Future enhancement: Could check if instructor assignment changed
        // and validate new instructor availability
    }

    /**
     * Handles class definition deactivation events.
     * Logs when a class definition is deactivated.
     *
     * @param event The class definition deactivated event
     */
    @EventListener
    public void handleClassDefinitionDeactivated(ClassDefinitionDeactivatedEventDTO event) {
        log.info("Class definition deactivated: '{}' - UUID: {}", 
                event.title(), 
                event.definitionUuid());
                
        // Future enhancement: Could free up instructor availability
        // or adjust scheduling recommendations
    }
}