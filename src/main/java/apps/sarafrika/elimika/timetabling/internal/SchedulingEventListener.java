package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.event.classes.ClassDefinedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionDeactivatedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionUpdatedEventDTO;
import apps.sarafrika.elimika.shared.event.availability.InstructorAvailabilityChangedEventDTO;
import apps.sarafrika.elimika.timetabling.dto.ClassScheduledEventDTO;
import apps.sarafrika.elimika.timetabling.dto.StudentEnrolledEventDTO;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event listener for timetabling-related events from other modules.
 * <p>
 * This component handles events from Classes, Availability, and Student modules
 * to react to changes that may affect scheduled instances and enrollments.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulingEventListener {

    private final ScheduledInstanceRepository scheduledInstanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles class definition creation events.
     * Pre-validates scheduling requirements and caches class definition data.
     *
     * @param event The class defined event
     */
    @EventListener
    @Async
    public void handleClassDefined(ClassDefinedEventDTO event) {
        log.info("Class '{}' defined with UUID: {} - validating scheduling requirements", 
                event.title(), 
                event.definitionUuid());
                
        // Pre-validate that the default instructor exists and has some availability
        if (event.defaultInstructorUuid() != null) {
            log.debug("Pre-validating instructor {} availability for new class definition {}", 
                    event.defaultInstructorUuid(),
                    event.definitionUuid());
                    
            // Check if instructor has any availability patterns set by checking if they have any scheduled instances
            // This is a heuristic - if they have scheduled instances, they likely have availability
            long existingInstances = scheduledInstanceRepository.countInstancesForInstructorInPeriod(
                    event.defaultInstructorUuid(),
                    LocalDateTime.now().minusDays(30),
                    LocalDateTime.now().plusDays(30));
            
            if (existingInstances == 0) {
                log.warn("Instructor {} has no recent scheduled instances - may need availability configuration",
                        event.defaultInstructorUuid());
            }
        }
        
        log.debug("Class definition {} ready for scheduling with title: '{}', location: {}, capacity: {}", 
                event.definitionUuid(),
                event.title(),
                event.locationType(),
                event.maxParticipants());
    }

    /**
     * Handles class definition update events.
     * Updates denormalized fields in existing scheduled instances.
     *
     * @param event The class definition updated event
     */
    @EventListener
    @Transactional
    @Async
    public void handleClassDefinitionUpdated(ClassDefinitionUpdatedEventDTO event) {
        log.info("Class definition updated: '{}' - updating scheduled instances", 
                event.title(), 
                event.definitionUuid());
                
        // Find all future scheduled instances for this class definition
        List<ScheduledInstance> futureInstances = scheduledInstanceRepository
                .findByClassDefinitionAndTimeRange(
                        event.definitionUuid(), 
                        LocalDateTime.now(), 
                        LocalDateTime.now().plusYears(1));
        
        // Update denormalized fields in scheduled instances
        futureInstances.forEach(instance -> {
            boolean updated = false;
            
            if (!event.title().equals(instance.getTitle())) {
                instance.setTitle(event.title());
                updated = true;
                log.debug("Updated title for scheduled instance {}", instance.getUuid());
            }
            
            if (updated) {
                scheduledInstanceRepository.save(instance);
            }
        });
        
        log.info("Updated {} scheduled instances for class definition {}", 
                futureInstances.size(), event.definitionUuid());
    }

    /**
     * Handles class definition deactivation events.
     * Cancels future scheduled instances for deactivated class definitions.
     *
     * @param event The class definition deactivated event
     */
    @EventListener
    @Transactional
    @Async
    public void handleClassDefinitionDeactivated(ClassDefinitionDeactivatedEventDTO event) {
        log.info("Class definition deactivated: '{}' - cancelling future scheduled instances", 
                event.title(), 
                event.definitionUuid());
                
        // Find all future scheduled instances for this class definition
        List<ScheduledInstance> futureInstances = scheduledInstanceRepository
                .findByClassDefinitionAndTimeRange(
                        event.definitionUuid(), 
                        LocalDateTime.now(), 
                        LocalDateTime.now().plusYears(1));
        
        // Cancel each future scheduled instance
        futureInstances.forEach(instance -> {
            if (SchedulingStatus.SCHEDULED.equals(instance.getStatus())) {
                instance.setStatus(SchedulingStatus.CANCELLED);
                instance.setCancellationReason("Class definition deactivated");
                scheduledInstanceRepository.save(instance);
                
                // Cancel all enrollments for this instance
                List<Enrollment> enrollments = enrollmentRepository
                        .findByScheduledInstanceUuidAndStatus(instance.getUuid(), EnrollmentStatus.ENROLLED);
                
                enrollments.forEach(enrollment -> {
                    enrollment.setStatus(EnrollmentStatus.CANCELLED);
                    enrollmentRepository.save(enrollment);
                });
                
                log.debug("Cancelled scheduled instance {} and {} enrollments", 
                        instance.getUuid(), enrollments.size());
            }
        });
        
        log.info("Cancelled {} future scheduled instances for deactivated class {}", 
                futureInstances.size(), event.definitionUuid());
    }

    /**
     * Handles instructor availability change events.
     * Checks for potential conflicts with existing scheduled instances and publishes conflict events.
     *
     * @param event The instructor availability changed event
     */
    @EventListener
    @Async
    public void handleInstructorAvailabilityChanged(InstructorAvailabilityChangedEventDTO event) {
        log.info("Instructor availability changed for instructor: {} on date: {}", 
                event.instructorUuid(), event.effectiveDate());
        
        // Find scheduled instances for this instructor on the affected date
        LocalDateTime startOfDay = event.effectiveDate().atStartOfDay();
        LocalDateTime endOfDay = event.effectiveDate().plusDays(1).atStartOfDay();
        
        List<ScheduledInstance> instancesOnDate = scheduledInstanceRepository
                .findByInstructorAndTimeRange(event.instructorUuid(), startOfDay, endOfDay);
        
        if (!instancesOnDate.isEmpty()) {
            log.warn("Found {} scheduled instances for instructor {} on date {} - checking for conflicts", 
                    instancesOnDate.size(), event.instructorUuid(), event.effectiveDate());
                    
            // Check each scheduled instance against the new availability
            instancesOnDate.forEach(instance -> {
                if (SchedulingStatus.SCHEDULED.equals(instance.getStatus()) || 
                    SchedulingStatus.ONGOING.equals(instance.getStatus())) {
                    
                    log.warn("Potential scheduling conflict detected for instance {} at {} - {}", 
                            instance.getUuid(), 
                            instance.getStartTime(), 
                            instance.getEndTime());
                    
                    // Publish PotentialConflictDetected event
                    PotentialConflictDetectedEvent conflictEvent = new PotentialConflictDetectedEvent(
                            instance.getUuid(),
                            event.instructorUuid(),
                            instance.getStartTime(),
                            instance.getEndTime(),
                            "Instructor availability changed",
                            event.effectiveDate()
                    );
                    eventPublisher.publishEvent(conflictEvent);
                }
            });
        } else {
            log.debug("No scheduled instances found for instructor {} on {}", 
                    event.instructorUuid(), event.effectiveDate());
        }
    }


    /**
     * Handles system time-based events for status updates.
     * Updates scheduled instance statuses based on current time and publishes status change events.
     */
    @Transactional
    public void performStatusUpdateCheck() {
        log.debug("Performing scheduled status update check at {}", LocalDateTime.now());
        
        LocalDateTime now = LocalDateTime.now();
        
        // Find scheduled instances that should transition to ONGOING
        List<ScheduledInstance> shouldBeOngoing = scheduledInstanceRepository
                .findScheduledInstancesPastStartTime(now);
        
        shouldBeOngoing.forEach(instance -> {
            instance.setStatus(SchedulingStatus.ONGOING);
            scheduledInstanceRepository.save(instance);
            log.debug("Updated scheduled instance {} to ONGOING", instance.getUuid());
            
            // Publish ScheduledInstanceStarted event
            ScheduledInstanceStartedEvent startedEvent = new ScheduledInstanceStartedEvent(
                    instance.getUuid(),
                    instance.getClassDefinitionUuid(),
                    instance.getInstructorUuid(),
                    instance.getTitle(),
                    instance.getStartTime(),
                    instance.getEndTime(),
                    now
            );
            eventPublisher.publishEvent(startedEvent);
        });
        
        // Find ongoing instances that should transition to COMPLETED
        List<ScheduledInstance> shouldBeCompleted = scheduledInstanceRepository
                .findOngoingInstancesPastEndTime(now);
        
        shouldBeCompleted.forEach(instance -> {
            instance.setStatus(SchedulingStatus.COMPLETED);
            scheduledInstanceRepository.save(instance);
            log.debug("Updated scheduled instance {} to COMPLETED", instance.getUuid());
            
            // Publish ScheduledInstanceCompleted event
            ScheduledInstanceCompletedEvent completedEvent = new ScheduledInstanceCompletedEvent(
                    instance.getUuid(),
                    instance.getClassDefinitionUuid(),
                    instance.getInstructorUuid(),
                    instance.getTitle(),
                    instance.getStartTime(),
                    instance.getEndTime(),
                    now
            );
            eventPublisher.publishEvent(completedEvent);
        });
        
        if (!shouldBeOngoing.isEmpty() || !shouldBeCompleted.isEmpty()) {
            log.info("Status update check completed: {} transitioned to ONGOING, {} transitioned to COMPLETED",
                    shouldBeOngoing.size(), shouldBeCompleted.size());
        }
    }

    // Inner event classes for events published by this listener

    /**
     * Event published when a potential scheduling conflict is detected.
     */
    public static record PotentialConflictDetectedEvent(
            java.util.UUID instanceUuid,
            java.util.UUID instructorUuid,
            LocalDateTime conflictStart,
            LocalDateTime conflictEnd,
            String reason,
            java.time.LocalDate affectedDate
    ) {}

    /**
     * Event published when a student enrollment is cancelled.
     */
    public static record StudentEnrollmentCancelledEvent(
            java.util.UUID enrollmentUuid,
            java.util.UUID instanceUuid,
            java.util.UUID studentUuid,
            java.util.UUID classDefinitionUuid,
            java.util.UUID instructorUuid,
            String cancellationReason,
            String classTitle,
            LocalDateTime classStartTime
    ) {}

    /**
     * Event published when a scheduled instance transitions to ONGOING status.
     */
    public static record ScheduledInstanceStartedEvent(
            java.util.UUID instanceUuid,
            java.util.UUID classDefinitionUuid,
            java.util.UUID instructorUuid,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime transitionTime
    ) {}

    /**
     * Event published when a scheduled instance transitions to COMPLETED status.
     */
    public static record ScheduledInstanceCompletedEvent(
            java.util.UUID instanceUuid,
            java.util.UUID classDefinitionUuid,
            java.util.UUID instructorUuid,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime transitionTime
    ) {}
}