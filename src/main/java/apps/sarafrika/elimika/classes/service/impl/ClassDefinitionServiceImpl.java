package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.*;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionUpdatedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassDefinitionDeactivatedEventDTO;
import apps.sarafrika.elimika.classes.factory.ClassDefinitionFactory;
import apps.sarafrika.elimika.classes.factory.RecurrencePatternFactory;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.RecurrencePatternRepository;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseTrainingApprovalSpi;
import apps.sarafrika.elimika.classes.exception.SchedulingConflictException;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.classes.util.enums.RecurrenceType;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final TimetableService timetableService;

    private static final String CLASS_DEFINITION_NOT_FOUND_TEMPLATE = "Class definition with UUID %s not found";
    private static final String RECURRENCE_PATTERN_NOT_FOUND_TEMPLATE = "Recurrence pattern with UUID %s not found";
    private static final int MAX_SCHEDULING_ITERATIONS = 2000;
    private static final int MAX_ROLLOVER_ITERATIONS = 20;

    @Override
    public ClassDefinitionCreationResponseDTO createClassDefinition(ClassDefinitionDTO classDefinitionDTO) {
        log.debug("Creating class definition with title: {}", classDefinitionDTO.title());

        if (classDefinitionDTO.sessionTemplates() == null || classDefinitionDTO.sessionTemplates().isEmpty()) {
            throw new IllegalArgumentException("At least one session template must be provided");
        }
        
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

        validateLocationRequirements(entity);
        validateTrainingApprovals(entity);
        validateTrainingFee(entity);

        ClassDefinition savedEntity = classDefinitionRepository.save(entity);
        ClassDefinitionDTO result = ClassDefinitionFactory.toDTO(savedEntity);

        ClassSchedulingOutcome schedulingOutcome = applySessionTemplates(result, classDefinitionDTO.sessionTemplates());
        if (schedulingOutcome.blockingConflict()) {
            throw new SchedulingConflictException(
                    String.format("Conflicts detected for class %s", result.title()),
                    schedulingOutcome.conflicts());
        }
        
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
        return new ClassDefinitionCreationResponseDTO(result, schedulingOutcome.scheduledInstances(), schedulingOutcome.conflicts());
    }

    private ClassSchedulingOutcome applySessionTemplates(ClassDefinitionDTO classDefinition,
                                                         List<ClassSessionTemplateDTO> templates) {
        List<ScheduledInstanceDTO> scheduledInstances = new ArrayList<>();
        List<ClassSchedulingConflictDTO> conflicts = new ArrayList<>();
        boolean blockingConflict = false;

        for (ClassSessionTemplateDTO template : templates) {
            if (template == null) {
                continue;
            }
            if (template.startTime() == null || template.endTime() == null) {
                throw new IllegalArgumentException("Session templates require both start_time and end_time");
            }
            if (!template.startTime().isBefore(template.endTime())) {
                throw new IllegalArgumentException("Session template start_time must be before end_time");
            }

            ConflictResolutionStrategy strategy = Optional.ofNullable(template.conflictResolution())
                    .orElse(ConflictResolutionStrategy.FAIL);
            int conflictCountBefore = conflicts.size();

            ClassRecurrenceDTO recurrence = template.recurrence();
            if (recurrence == null || recurrence.recurrenceType() == null) {
                scheduleSingleSession(classDefinition, template.startTime(), template.endTime(), strategy, conflicts, scheduledInstances);
            } else {
                scheduleRecurringSessions(classDefinition, template, recurrence, strategy, conflicts, scheduledInstances);
            }

            if (strategy == ConflictResolutionStrategy.FAIL && conflicts.size() > conflictCountBefore) {
                blockingConflict = true;
            }
        }

        return new ClassSchedulingOutcome(scheduledInstances, conflicts, blockingConflict);
    }

    private void scheduleSingleSession(ClassDefinitionDTO classDefinition,
                                       LocalDateTime start,
                                       LocalDateTime end,
                                       ConflictResolutionStrategy strategy,
                                       List<ClassSchedulingConflictDTO> conflicts,
                                       List<ScheduledInstanceDTO> scheduledInstances) {
        List<String> reasons = detectConflicts(classDefinition, start, end);
        if (reasons.isEmpty()) {
            scheduledInstances.add(scheduleInstance(classDefinition, start, end));
            return;
        }

        conflicts.add(new ClassSchedulingConflictDTO(start, end, reasons));
        if (strategy == ConflictResolutionStrategy.ROLLOVER) {
            attemptRollover(classDefinition, start, end, null, conflicts, scheduledInstances);
        }
    }

    private void scheduleRecurringSessions(ClassDefinitionDTO classDefinition,
                                           ClassSessionTemplateDTO template,
                                           ClassRecurrenceDTO recurrence,
                                           ConflictResolutionStrategy strategy,
                                           List<ClassSchedulingConflictDTO> conflicts,
                                           List<ScheduledInstanceDTO> scheduledInstances) {
        RecurrenceType type = recurrence.recurrenceType();
        int targetOccurrences = recurrence.occurrenceCount() != null ? recurrence.occurrenceCount() : 0;
        LocalDate endDateLimit = recurrence.endDate();

        switch (type) {
            case DAILY -> scheduleDaily(classDefinition, template, recurrence, strategy, conflicts, scheduledInstances, targetOccurrences, endDateLimit);
            case WEEKLY -> scheduleWeekly(classDefinition, template, recurrence, strategy, conflicts, scheduledInstances, targetOccurrences, endDateLimit);
            case MONTHLY -> scheduleMonthly(classDefinition, template, recurrence, strategy, conflicts, scheduledInstances, targetOccurrences, endDateLimit);
            default -> scheduleSingleSession(classDefinition, template.startTime(), template.endTime(), strategy, conflicts, scheduledInstances);
        }
    }

    private void scheduleDaily(ClassDefinitionDTO classDefinition,
                               ClassSessionTemplateDTO template,
                               ClassRecurrenceDTO recurrence,
                               ConflictResolutionStrategy strategy,
                               List<ClassSchedulingConflictDTO> conflicts,
                               List<ScheduledInstanceDTO> scheduledInstances,
                               int targetOccurrences,
                               LocalDate endDateLimit) {
        int interval = Optional.ofNullable(recurrence.intervalValue()).orElse(1);
        LocalDateTime cursorStart = template.startTime();
        LocalDateTime cursorEnd = template.endTime();
        int scheduledCount = 0;
        int iterations = 0;

        while (shouldContinueRecurrence(scheduledCount, targetOccurrences, cursorStart.toLocalDate(), endDateLimit) &&
                iterations < MAX_SCHEDULING_ITERATIONS) {
            iterations++;
            boolean scheduled = attemptScheduleWindow(classDefinition, cursorStart, cursorEnd, conflicts, scheduledInstances);
            if (scheduled) {
                scheduledCount++;
            } else if (strategy == ConflictResolutionStrategy.SKIP) {
                scheduledCount++;
            } else if (strategy == ConflictResolutionStrategy.ROLLOVER) {
                if (attemptRollover(classDefinition, cursorStart, cursorEnd, recurrence, conflicts, scheduledInstances)) {
                    scheduledCount++;
                } else {
                    scheduledCount++;
                }
            }

            cursorStart = cursorStart.plusDays(interval);
            cursorEnd = cursorEnd.plusDays(interval);
        }
    }

    private void scheduleWeekly(ClassDefinitionDTO classDefinition,
                                ClassSessionTemplateDTO template,
                                ClassRecurrenceDTO recurrence,
                                ConflictResolutionStrategy strategy,
                                List<ClassSchedulingConflictDTO> conflicts,
                                List<ScheduledInstanceDTO> scheduledInstances,
                                int targetOccurrences,
                                LocalDate endDateLimit) {
        int interval = Optional.ofNullable(recurrence.intervalValue()).orElse(1);
        Set<DayOfWeek> allowedDays = parseDaysOfWeek(recurrence.daysOfWeek());
        if (allowedDays.isEmpty()) {
            allowedDays = Set.of(template.startTime().getDayOfWeek());
        }

        LocalDate cursorDate = template.startTime().toLocalDate();
        LocalDate weekAnchor = cursorDate.minusDays(cursorDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        int scheduledCount = 0;
        int iterations = 0;
        while (shouldContinueRecurrence(scheduledCount, targetOccurrences, cursorDate, endDateLimit) &&
                iterations < MAX_SCHEDULING_ITERATIONS) {
            iterations++;
            LocalDate currentWeekStart = cursorDate.minusDays(cursorDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
            long weeksBetween = ChronoUnit.WEEKS.between(weekAnchor, currentWeekStart);

            if (weeksBetween % interval == 0 && allowedDays.contains(cursorDate.getDayOfWeek())) {
                LocalDateTime start = cursorDate.atTime(template.startTime().toLocalTime());
                LocalDateTime end = cursorDate.atTime(template.endTime().toLocalTime());

                boolean scheduled = attemptScheduleWindow(classDefinition, start, end, conflicts, scheduledInstances);
                if (scheduled) {
                    scheduledCount++;
                } else if (strategy == ConflictResolutionStrategy.SKIP) {
                    scheduledCount++;
                } else if (strategy == ConflictResolutionStrategy.ROLLOVER) {
                    if (attemptRollover(classDefinition, start, end, recurrence, conflicts, scheduledInstances)) {
                        scheduledCount++;
                    } else {
                        scheduledCount++;
                    }
                }
            }

            cursorDate = cursorDate.plusDays(1);
        }
    }

    private void scheduleMonthly(ClassDefinitionDTO classDefinition,
                                 ClassSessionTemplateDTO template,
                                 ClassRecurrenceDTO recurrence,
                                 ConflictResolutionStrategy strategy,
                                 List<ClassSchedulingConflictDTO> conflicts,
                                 List<ScheduledInstanceDTO> scheduledInstances,
                                 int targetOccurrences,
                                 LocalDate endDateLimit) {
        int interval = Optional.ofNullable(recurrence.intervalValue()).orElse(1);
        int scheduledCount = 0;
        int iterations = 0;

        LocalDate cursorDate = template.startTime().toLocalDate();
        while (shouldContinueRecurrence(scheduledCount, targetOccurrences, cursorDate, endDateLimit) &&
                iterations < MAX_SCHEDULING_ITERATIONS) {
            iterations++;
            LocalDate targetDate = resolveMonthlyDate(cursorDate, recurrence.dayOfMonth());
            LocalDateTime start = LocalDateTime.of(targetDate, template.startTime().toLocalTime());
            LocalDateTime end = LocalDateTime.of(targetDate, template.endTime().toLocalTime());

            boolean scheduled = attemptScheduleWindow(classDefinition, start, end, conflicts, scheduledInstances);
            if (scheduled) {
                scheduledCount++;
            } else if (strategy == ConflictResolutionStrategy.SKIP) {
                scheduledCount++;
            } else if (strategy == ConflictResolutionStrategy.ROLLOVER) {
                if (attemptRollover(classDefinition, start, end, recurrence, conflicts, scheduledInstances)) {
                    scheduledCount++;
                } else {
                    scheduledCount++;
                }
            }

            cursorDate = cursorDate.plusMonths(interval);
        }
    }

    private boolean attemptScheduleWindow(ClassDefinitionDTO classDefinition,
                                          LocalDateTime start,
                                          LocalDateTime end,
                                          List<ClassSchedulingConflictDTO> conflicts,
                                          List<ScheduledInstanceDTO> scheduledInstances) {
        List<String> reasons = detectConflicts(classDefinition, start, end);
        if (reasons.isEmpty()) {
            scheduledInstances.add(scheduleInstance(classDefinition, start, end));
            return true;
        }

        conflicts.add(new ClassSchedulingConflictDTO(start, end, reasons));
        return false;
    }

    private boolean attemptRollover(ClassDefinitionDTO classDefinition,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    ClassRecurrenceDTO recurrence,
                                    List<ClassSchedulingConflictDTO> conflicts,
                                    List<ScheduledInstanceDTO> scheduledInstances) {
        ClassRecurrenceDTO safeRecurrence = recurrence != null ? recurrence :
                new ClassRecurrenceDTO(RecurrenceType.DAILY, 1, null, null, null, 1);
        LocalDateTime rollingStart = start;
        LocalDateTime rollingEnd = end;
        int attempts = 0;

        while (attempts < MAX_ROLLOVER_ITERATIONS) {
            attempts++;
            rollingStart = advanceByRecurrence(rollingStart, safeRecurrence);
            rollingEnd = advanceByRecurrence(rollingEnd, safeRecurrence);

            List<String> reasons = detectConflicts(classDefinition, rollingStart, rollingEnd);
            if (reasons.isEmpty()) {
                scheduledInstances.add(scheduleInstance(classDefinition, rollingStart, rollingEnd));
                return true;
            }
            conflicts.add(new ClassSchedulingConflictDTO(rollingStart, rollingEnd, reasons));
        }
        return false;
    }

    private LocalDateTime advanceByRecurrence(LocalDateTime current, ClassRecurrenceDTO recurrence) {
        int interval = Optional.ofNullable(recurrence.intervalValue()).orElse(1);
        return switch (recurrence.recurrenceType()) {
            case DAILY -> current.plusDays(interval);
            case WEEKLY -> current.plusWeeks(interval);
            case MONTHLY -> current.plusMonths(interval);
            default -> current.plusDays(interval);
        };
    }

    private boolean shouldContinueRecurrence(int scheduledCount,
                                             int targetOccurrences,
                                             LocalDate currentDate,
                                             LocalDate endDateLimit) {
        if (targetOccurrences > 0 && scheduledCount < targetOccurrences) {
            return true;
        }
        if (targetOccurrences <= 0 && endDateLimit != null) {
            return !currentDate.isAfter(endDateLimit);
        }
        return targetOccurrences <= 0 && endDateLimit == null && scheduledCount == 0;
    }

    private LocalDate resolveMonthlyDate(LocalDate baseDate, Integer dayOfMonth) {
        if (dayOfMonth == null) {
            return baseDate;
        }
        int lastDay = baseDate.lengthOfMonth();
        int safeDay = Math.min(dayOfMonth, lastDay);
        return baseDate.withDayOfMonth(safeDay);
    }

    private Set<DayOfWeek> parseDaysOfWeek(String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return Set.of();
        }
        String[] parts = daysOfWeek.split(",");
        Set<DayOfWeek> results = new LinkedHashSet<>();
        for (String part : parts) {
            try {
                results.add(DayOfWeek.valueOf(part.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                log.warn("Ignoring invalid day_of_week entry: {}", part);
            }
        }
        return results;
    }

    private List<String> detectConflicts(ClassDefinitionDTO classDefinition, LocalDateTime start, LocalDateTime end) {
        List<String> reasons = new ArrayList<>();
        UUID instructorUuid = classDefinition.defaultInstructorUuid();

        if (!availabilityService.isInstructorAvailable(instructorUuid, start, end)) {
            reasons.add("Instructor is not available for the requested time range");
        }
        ScheduleRequestDTO requestDTO = new ScheduleRequestDTO(
                classDefinition.uuid(),
                instructorUuid,
                start,
                end,
                "UTC"
        );
        if (timetableService.hasInstructorConflict(instructorUuid, requestDTO)) {
            reasons.add("Instructor has overlapping scheduled instances");
        }
        return reasons;
    }

    private ScheduledInstanceDTO scheduleInstance(ClassDefinitionDTO classDefinition, LocalDateTime start, LocalDateTime end) {
        ScheduleRequestDTO scheduleRequestDTO = new ScheduleRequestDTO(
                classDefinition.uuid(),
                classDefinition.defaultInstructorUuid(),
                start,
                end,
                "UTC"
        );
        return timetableService.scheduleClass(scheduleRequestDTO);
    }

    private record ClassSchedulingOutcome(List<ScheduledInstanceDTO> scheduledInstances,
                                          List<ClassSchedulingConflictDTO> conflicts,
                                          boolean blockingConflict) {
    }

    @Override
    public ClassDefinitionDTO updateClassDefinition(UUID definitionUuid, ClassDefinitionDTO classDefinitionDTO) {
        log.debug("Updating class definition with UUID: {}", definitionUuid);
        
        ClassDefinition existingEntity = classDefinitionRepository.findByUuid(definitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_DEFINITION_NOT_FOUND_TEMPLATE, definitionUuid)));
        
        ClassDefinitionFactory.updateEntityFromDTO(existingEntity, classDefinitionDTO);
        validateLocationRequirements(existingEntity);
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
    @Transactional(readOnly = true)
    public RecurrencePatternDTO getRecurrencePattern(UUID patternUuid) {
        log.debug("Retrieving recurrence pattern with UUID: {}", patternUuid);
        
        return recurrencePatternRepository.findByUuid(patternUuid)
                .map(RecurrencePatternFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(RECURRENCE_PATTERN_NOT_FOUND_TEMPLATE, patternUuid)));
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

        SessionFormat sessionFormat = entity.getSessionFormat();
        LocationType locationType = entity.getLocationType();

        if (entity.getDefaultInstructorUuid() != null) {
            Optional<BigDecimal> instructorRate = courseTrainingApprovalSpi.resolveInstructorRate(
                    courseUuid,
                    entity.getDefaultInstructorUuid(),
                    sessionFormat,
                    locationType
            );
            if (instructorRate.isPresent()) {
                return instructorRate;
            }
        }

        if (entity.getOrganisationUuid() != null) {
            return courseTrainingApprovalSpi.resolveOrganisationRate(
                    courseUuid,
                    entity.getOrganisationUuid(),
                    sessionFormat,
                    locationType
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
        if (entity.getLocationType() == null) {
            throw new IllegalArgumentException("Location type is required when linking a class definition to a course");
        }

        BigDecimal resolvedRate = resolveApprovedRate(entity)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "No approved rate card found for the selected instructor/organisation on course %s. Submit and approve a training application with rates first.",
                        entity.getCourseUuid())));

        if (entity.getTrainingFee() == null) {
            entity.setTrainingFee(resolvedRate);
        } else if (entity.getTrainingFee().compareTo(resolvedRate) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Training fee %.2f must match the approved rate card amount %.2f for %s %s delivery.",
                    entity.getTrainingFee(),
                    resolvedRate,
                    entity.getSessionFormat(),
                    entity.getLocationType()));
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

    /**
     * Ensures that in-person and hybrid classes carry a Mapbox-ready location payload.
     * <p>
     * ONLINE classes may omit location_name and coordinates; IN_PERSON and HYBRID must supply:
     * - location_name (human readable)
     * - location_latitude (between -90 and 90)
     * - location_longitude (between -180 and 180)
     *
     * @param entity the class definition to validate
     */
    private void validateLocationRequirements(ClassDefinition entity) {
        LocationType locationType = entity.getLocationType();
        if (locationType == null) {
            return;
        }

        if (LocationType.ONLINE.equals(locationType)) {
            // Online classes do not require a physical location; location fields are optional
            return;
        }

        String locationName = entity.getLocationName();
        if (locationName == null || locationName.trim().isEmpty()) {
            throw new IllegalArgumentException("location_name is required when location_type is IN_PERSON or HYBRID");
        }

        var latitude = entity.getLocationLatitude();
        var longitude = entity.getLocationLongitude();
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("location_latitude and location_longitude are required when location_type is IN_PERSON or HYBRID");
        }

        if (latitude.compareTo(new BigDecimal("-90")) < 0 || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("location_latitude must be between -90 and 90 degrees");
        }
        if (longitude.compareTo(new BigDecimal("-180")) < 0 || longitude.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException("location_longitude must be between -180 and 180 degrees");
        }
    }
}
