package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionUpdatedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionDeactivatedEventDTO;
import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;
import apps.sarafrika.elimika.classes.factory.ClassDefinitionFactory;
import apps.sarafrika.elimika.classes.factory.RecurrencePatternFactory;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.RecurrencePattern;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.RecurrencePatternRepository;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassDefinitionServiceImpl implements ClassDefinitionServiceInterface, ClassDefinitionService {

    private final ClassDefinitionRepository classDefinitionRepository;
    private final RecurrencePatternRepository recurrencePatternRepository;
    private final AvailabilityService availabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseInfoService courseInfoService;
    private final CourseTrainingApprovalSpi courseTrainingApprovalSpi;

    private static final String CLASS_DEFINITION_NOT_FOUND_TEMPLATE = "Class definition with UUID %s not found";
    private static final String RECURRENCE_PATTERN_NOT_FOUND_TEMPLATE = "Recurrence pattern with UUID %s not found";

    @Override
    public ClassDefinitionDTO createClassDefinition(ClassDefinitionDTO classDefinitionDTO) {
        log.debug("Creating class definition with title: {}", classDefinitionDTO.title());
        
        ClassDefinition entity = ClassDefinitionFactory.toEntity(classDefinitionDTO);
        
        // Validate instructor availability
        validateInstructorAvailability(entity.getDefaultInstructorUuid());
        
        // Set defaults
        if (entity.getMaxParticipants() == null) {
            entity.setMaxParticipants(50);
        }
        if (entity.getAllowWaitlist() == null) {
            entity.setAllowWaitlist(true);
        }
        if (entity.getIsActive() == null) {
            entity.setIsActive(true);
        }
        
        validateTrainingApprovals(entity);
        validateTrainingFee(entity);

        ClassDefinition savedEntity = classDefinitionRepository.save(entity);
        ClassDefinitionDTO result = ClassDefinitionFactory.toDTO(savedEntity);
        
        // Publish domain event
        ClassDefinedEventDTO event = new ClassDefinedEventDTO(
                result.uuid(),
                result.title(),
                (int) result.getDurationMinutes(),
                result.defaultInstructorUuid(),
                result.courseUuid(),
                result.organisationUuid(),
                result.locationType(),
                result.maxParticipants(),
                result.allowWaitlist(),
                result.recurrencePatternUuid()
        );
        eventPublisher.publishEvent(event);
        
        log.info("Created class definition with UUID: {} and published ClassDefinedEvent", result.uuid());
        return result;
    }

    @Override
    public ClassDefinitionDTO updateClassDefinition(UUID definitionUuid, ClassDefinitionDTO classDefinitionDTO) {
        log.debug("Updating class definition with UUID: {}", definitionUuid);
        
        ClassDefinition existingEntity = classDefinitionRepository.findByUuid(definitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, definitionUuid)));
        
        ClassDefinitionFactory.updateEntityFromDTO(existingEntity, classDefinitionDTO);
        validateTrainingApprovals(existingEntity);
        validateTrainingFee(existingEntity);
        
        ClassDefinition savedEntity = classDefinitionRepository.save(existingEntity);
        ClassDefinitionDTO result = ClassDefinitionFactory.toDTO(savedEntity);
        
        // Publish domain event
        ClassDefinitionUpdatedEventDTO event = new ClassDefinitionUpdatedEventDTO(
                result.uuid(),
                result.title()
        );
        eventPublisher.publishEvent(event);
        
        log.info("Updated class definition with UUID: {} and published ClassDefinitionUpdatedEvent", definitionUuid);
        return result;
    }

    @Override
    public void deactivateClassDefinition(UUID definitionUuid) {
        log.debug("Deactivating class definition with UUID: {}", definitionUuid);
        
        ClassDefinition entity = classDefinitionRepository.findByUuid(definitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, definitionUuid)));
        
        String title = entity.getTitle();
        entity.setIsActive(false);
        classDefinitionRepository.save(entity);
        
        // Publish domain event
        ClassDefinitionDeactivatedEventDTO event = new ClassDefinitionDeactivatedEventDTO(
                definitionUuid,
                title
        );
        eventPublisher.publishEvent(event);
        
        log.info("Deactivated class definition with UUID: {} and published ClassDefinitionDeactivatedEvent", definitionUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassDefinitionDTO getClassDefinition(UUID definitionUuid) {
        log.debug("Retrieving class definition with UUID: {}", definitionUuid);
        
        return classDefinitionRepository.findByUuid(definitionUuid)
                .map(ClassDefinitionFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, definitionUuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionDTO> findClassesForCourse(UUID courseUuid) {
        log.debug("Finding classes for course UUID: {}", courseUuid);
        
        return classDefinitionRepository.findByCourseUuid(courseUuid)
                .stream()
                .map(ClassDefinitionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionDTO> findActiveClassesForCourse(UUID courseUuid) {
        log.debug("Finding active classes for course UUID: {}", courseUuid);
        
        return classDefinitionRepository.findActiveClassesForCourse(courseUuid)
                .stream()
                .map(ClassDefinitionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionDTO> findClassesForInstructor(UUID instructorUuid) {
        log.debug("Finding classes for instructor UUID: {}", instructorUuid);
        
        return classDefinitionRepository.findByDefaultInstructorUuid(instructorUuid)
                .stream()
                .map(ClassDefinitionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionDTO> findActiveClassesForInstructor(UUID instructorUuid) {
        log.debug("Finding active classes for instructor UUID: {}", instructorUuid);
        
        return classDefinitionRepository.findActiveClassesForInstructor(instructorUuid)
                .stream()
                .map(ClassDefinitionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionDTO> findClassesForOrganisation(UUID organisationUuid) {
        log.debug("Finding classes for organisation UUID: {}", organisationUuid);
        
        return classDefinitionRepository.findByOrganisationUuid(organisationUuid)
                .stream()
                .map(ClassDefinitionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassDefinitionDTO> findAllActiveClasses() {
        log.debug("Finding all active classes");
        
        return classDefinitionRepository.findByIsActiveTrue()
                .stream()
                .map(ClassDefinitionFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RecurrencePatternDTO createRecurrencePattern(RecurrencePatternDTO recurrencePatternDTO) {
        log.debug("Creating recurrence pattern with type: {}", recurrencePatternDTO.recurrenceType());
        
        RecurrencePattern entity = RecurrencePatternFactory.toEntity(recurrencePatternDTO);
        
        // Set defaults
        if (entity.getIntervalValue() == null) {
            entity.setIntervalValue(1);
        }
        
        RecurrencePattern savedEntity = recurrencePatternRepository.save(entity);
        RecurrencePatternDTO result = RecurrencePatternFactory.toDTO(savedEntity);
        
        log.info("Created recurrence pattern with UUID: {}", result.uuid());
        return result;
    }

    @Override
    public RecurrencePatternDTO updateRecurrencePattern(UUID patternUuid, RecurrencePatternDTO recurrencePatternDTO) {
        log.debug("Updating recurrence pattern with UUID: {}", patternUuid);
        
        RecurrencePattern existingEntity = recurrencePatternRepository.findByUuid(patternUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(RECURRENCE_PATTERN_NOT_FOUND_TEMPLATE, patternUuid)));
        
        RecurrencePatternFactory.updateEntityFromDTO(existingEntity, recurrencePatternDTO);
        
        RecurrencePattern savedEntity = recurrencePatternRepository.save(existingEntity);
        RecurrencePatternDTO result = RecurrencePatternFactory.toDTO(savedEntity);
        
        log.info("Updated recurrence pattern with UUID: {}", patternUuid);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public RecurrencePatternDTO getRecurrencePattern(UUID patternUuid) {
        log.debug("Retrieving recurrence pattern with UUID: {}", patternUuid);
        
        return recurrencePatternRepository.findByUuid(patternUuid)
                .map(RecurrencePatternFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(RECURRENCE_PATTERN_NOT_FOUND_TEMPLATE, patternUuid)));
    }

    @Override
    public void deleteRecurrencePattern(UUID patternUuid) {
        log.debug("Deleting recurrence pattern with UUID: {}", patternUuid);
        
        RecurrencePattern entity = recurrencePatternRepository.findByUuid(patternUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(RECURRENCE_PATTERN_NOT_FOUND_TEMPLATE, patternUuid)));
        
        // Check if pattern is still in use by any class definitions
        List<ClassDefinition> dependentClasses = classDefinitionRepository.findAll()
                .stream()
                .filter(cd -> patternUuid.equals(cd.getRecurrencePatternUuid()))
                .toList();
        
        if (!dependentClasses.isEmpty()) {
            throw new IllegalStateException(
                String.format("Cannot delete recurrence pattern %s - it is still in use by %d class definition(s)", 
                    patternUuid, dependentClasses.size())
            );
        }
        
        recurrencePatternRepository.delete(entity);
        log.info("Deleted recurrence pattern with UUID: {}", patternUuid);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasInstructorAvailability(UUID instructorUuid) {
        log.debug("Checking if instructor {} has availability defined", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }

        try {
            var availability = availabilityService.getAvailabilityForInstructor(instructorUuid);
            boolean hasAvailability = !availability.isEmpty();
            log.debug("Instructor {} availability check: {}", instructorUuid, hasAvailability ? "HAS availability" : "NO availability");
            return hasAvailability;
        } catch (Exception e) {
            log.warn("Error checking availability for instructor {}: {}", instructorUuid, e.getMessage());
            return false;
        }
    }

    /**
     * Validates that an instructor has availability defined before creating a class.
     * Logs a warning if no availability is found but doesn't prevent class creation.
     *
     * @param instructorUuid The UUID of the instructor to validate
     */
    private void validateInstructorAvailability(UUID instructorUuid) {
        try {
            var availability = availabilityService.getAvailabilityForInstructor(instructorUuid);
            if (availability.isEmpty()) {
                log.warn("Instructor {} has no availability defined. Consider setting availability patterns before scheduling classes.", instructorUuid);
            } else {
                log.debug("Instructor {} has {} availability slots defined", instructorUuid, availability.size());
            }
        } catch (Exception e) {
            log.warn("Could not validate availability for instructor {}: {}", instructorUuid, e.getMessage());
        }
    }

    private Optional<BigDecimal> resolveApprovedRate(ClassDefinition entity) {
        UUID courseUuid = entity.getCourseUuid();
        if (courseUuid == null) {
            return Optional.empty();
        }

        if (entity.getDefaultInstructorUuid() != null) {
            Optional<BigDecimal> instructorRate = courseTrainingApprovalSpi.resolveInstructorRate(
                    courseUuid,
                    entity.getDefaultInstructorUuid(),
                    entity.getClassVisibility(),
                    entity.getSessionFormat()
            );
            if (instructorRate.isPresent()) {
                return instructorRate;
            }
        }

        if (entity.getOrganisationUuid() != null) {
            return courseTrainingApprovalSpi.resolveOrganisationRate(
                    courseUuid,
                    entity.getOrganisationUuid(),
                    entity.getClassVisibility(),
                    entity.getSessionFormat()
            );
        }

        return Optional.empty();
    }

    private void validateTrainingFee(ClassDefinition entity) {
        if (entity.getCourseUuid() == null) {
            return;
        }

        // Verify course exists and get minimum training fee via SPI
        BigDecimal minimumTrainingFee = courseInfoService.getMinimumTrainingFee(entity.getCourseUuid())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Course with UUID %s not found", entity.getCourseUuid())));

        if (entity.getClassVisibility() == null) {
            throw new IllegalArgumentException("Class visibility is required when linking a class definition to a course");
        }
        if (entity.getSessionFormat() == null) {
            throw new IllegalArgumentException("Session format is required when linking a class definition to a course");
        }

        BigDecimal resolvedRate = resolveApprovedRate(entity)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "No approved rate card found for the selected instructor/organisation on course %s. Submit and approve a training application with rates first.",
                        entity.getCourseUuid())));

        if (entity.getTrainingFee() == null) {
            entity.setTrainingFee(resolvedRate);
        } else if (entity.getTrainingFee().compareTo(resolvedRate) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Training fee %.2f must match the approved rate card amount %.2f for %s %s sessions.",
                    entity.getTrainingFee(),
                    resolvedRate,
                    entity.getClassVisibility(),
                    entity.getSessionFormat()));
        }

        if (entity.getTrainingFee().compareTo(minimumTrainingFee) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Training fee %.2f cannot be less than the course minimum training fee %.2f",
                    entity.getTrainingFee(), minimumTrainingFee));
        }
    }

    private void validateTrainingApprovals(ClassDefinition entity) {
        UUID courseUuid = entity.getCourseUuid();
        if (courseUuid == null) {
            return;
        }

        UUID instructorUuid = entity.getDefaultInstructorUuid();
        if (instructorUuid != null && !courseTrainingApprovalSpi.isInstructorApproved(courseUuid, instructorUuid)) {
            throw new IllegalStateException(String.format(
                    "Instructor %s is not approved to deliver course %s. Submit a training application and wait for approval before scheduling classes.",
                    instructorUuid, courseUuid));
        }

        UUID organisationUuid = entity.getOrganisationUuid();
        if (organisationUuid != null && !courseTrainingApprovalSpi.isOrganisationApproved(courseUuid, organisationUuid)) {
            throw new IllegalStateException(String.format(
                    "Organisation %s is not approved to deliver course %s. Submit a training application and wait for approval before scheduling classes.",
                    organisationUuid, courseUuid));
        }
    }
}
