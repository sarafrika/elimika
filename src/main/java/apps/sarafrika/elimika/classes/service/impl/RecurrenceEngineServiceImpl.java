package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.dto.RecurrencePatternDTO;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.RecurrenceEngineService;
import apps.sarafrika.elimika.classes.util.enums.RecurrenceType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of RecurrenceEngineService for generating recurring class schedules.
 * <p>
 * This service provides Google Calendar-like functionality for creating recurring class instances
 * from class definitions and recurrence patterns.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-06
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecurrenceEngineServiceImpl implements RecurrenceEngineService {

    private final ClassDefinitionServiceInterface classDefinitionService;
    private final TimetableService timetableService;
    private final AvailabilityService availabilityService;

    @Override
    public List<ScheduledInstanceDTO> generateScheduledInstances(
            ClassDefinitionDTO classDefinition,
            LocalDate scheduleStartDate,
            LocalDate scheduleEndDate) {
        
        log.debug("Generating scheduled instances for class: {} from {} to {}", 
                classDefinition.title(), scheduleStartDate, scheduleEndDate);
        
        validateClassDefinitionForScheduling(classDefinition);
        
        if (classDefinition.recurrencePatternUuid() == null) {
            throw new IllegalArgumentException("Class definition must have a recurrence pattern for scheduling");
        }
        
        // Get the recurrence pattern
        RecurrencePatternDTO recurrencePattern = classDefinitionService.getRecurrencePattern(
                classDefinition.recurrencePatternUuid());
        
        return generateScheduledInstances(classDefinition, recurrencePattern, scheduleStartDate, scheduleEndDate);
    }

    @Override
    public List<ScheduledInstanceDTO> generateScheduledInstances(
            ClassDefinitionDTO classDefinition,
            RecurrencePatternDTO recurrencePattern,
            LocalDate scheduleStartDate,
            LocalDate scheduleEndDate) {
        
        log.debug("Generating scheduled instances for class: {} with pattern: {} from {} to {}", 
                classDefinition.title(), recurrencePattern.recurrenceType(), scheduleStartDate, scheduleEndDate);
        
        validateClassDefinitionForScheduling(classDefinition);
        validateRecurrencePattern(recurrencePattern);
        
        // Calculate occurrence dates
        List<LocalDate> occurrenceDates = calculateOccurrenceDates(
                recurrencePattern, scheduleStartDate, scheduleEndDate);
        
        // Generate scheduled instances for each occurrence
        List<ScheduledInstanceDTO> scheduledInstances = new ArrayList<>();
        
        for (LocalDate occurrenceDate : occurrenceDates) {
            LocalDateTime startDateTime = occurrenceDate.atTime(classDefinition.defaultStartTime().toLocalTime());
            LocalDateTime endDateTime = occurrenceDate.atTime(classDefinition.defaultEndTime().toLocalTime());
            
            // Create ScheduleRequestDTO for this instance
            ScheduleRequestDTO scheduleRequest = new ScheduleRequestDTO(
                    classDefinition.uuid(),
                    classDefinition.defaultInstructorUuid(),
                    startDateTime,
                    endDateTime,
                    "UTC" // Default timezone - could be parameterized later
            );
            
            try {
                // Check for conflicts before creating
                if (!hasSchedulingConflict(scheduleRequest)) {
                    ScheduledInstanceDTO scheduledInstance = timetableService.scheduleClass(scheduleRequest);
                    scheduledInstances.add(scheduledInstance);
                    log.debug("Created scheduled instance for {}: {}", occurrenceDate, scheduledInstance.uuid());
                } else {
                    log.warn("Skipping scheduled instance for {} due to conflict", occurrenceDate);
                }
            } catch (Exception e) {
                log.error("Failed to create scheduled instance for {}: {}", occurrenceDate, e.getMessage());
            }
        }
        
        log.info("Generated {} scheduled instances for class: {}", 
                scheduledInstances.size(), classDefinition.title());
        
        return scheduledInstances;
    }

    @Override
    public List<ScheduledInstanceDTO> scheduleRecurringClass(
            UUID classDefinitionUuid,
            LocalDate scheduleStartDate,
            LocalDate scheduleEndDate) {
        
        log.debug("Scheduling recurring class: {} from {} to {}", 
                classDefinitionUuid, scheduleStartDate, scheduleEndDate);
        
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        
        ClassDefinitionDTO classDefinition = classDefinitionService.getClassDefinition(classDefinitionUuid);
        return generateScheduledInstances(classDefinition, scheduleStartDate, scheduleEndDate);
    }

    @Override
    public List<ScheduledInstanceDTO> updateRecurringSchedule(UUID classDefinitionUuid) {
        log.debug("Updating recurring schedule for class: {}", classDefinitionUuid);
        
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        
        ClassDefinitionDTO classDefinition = classDefinitionService.getClassDefinition(classDefinitionUuid);
        
        // Cancel future instances (from tomorrow onwards)
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int cancelledCount = cancelRecurringSchedule(classDefinitionUuid, "Schedule updated");
        
        log.info("Cancelled {} future instances for class update", cancelledCount);
        
        // Generate new schedule from tomorrow
        if (classDefinition.recurrencePatternUuid() != null) {
            RecurrencePatternDTO pattern = classDefinitionService.getRecurrencePattern(
                    classDefinition.recurrencePatternUuid());
            
            LocalDate scheduleEndDate = pattern.endDate() != null ? 
                    pattern.endDate() : tomorrow.plusYears(1); // Default to 1 year if no end date
            
            return generateScheduledInstances(classDefinition, pattern, tomorrow, scheduleEndDate);
        }
        
        return Collections.emptyList();
    }

    @Override
    public int cancelRecurringSchedule(UUID classDefinitionUuid, String reason) {
        log.debug("Cancelling recurring schedule for class: {} with reason: {}", classDefinitionUuid, reason);
        
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }
        
        // Get future scheduled instances for this class definition
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate futureLimit = tomorrow.plusYears(2); // Look ahead 2 years max
        
        List<ScheduledInstanceDTO> instructorSchedule = timetableService.getScheduleForInstructor(
                classDefinitionService.getClassDefinition(classDefinitionUuid).defaultInstructorUuid(),
                tomorrow, futureLimit);
        
        // Filter to only instances for this class definition that are still scheduled
        List<ScheduledInstanceDTO> futureSessions = instructorSchedule.stream()
                .filter(instance -> classDefinitionUuid.equals(instance.classDefinitionUuid()))
                .filter(instance -> "SCHEDULED".equals(instance.status()))
                .collect(Collectors.toList());
        
        // Cancel each future session
        int cancelledCount = 0;
        for (ScheduledInstanceDTO instance : futureSessions) {
            try {
                timetableService.cancelScheduledInstance(instance.uuid(), reason);
                cancelledCount++;
            } catch (Exception e) {
                log.error("Failed to cancel scheduled instance {}: {}", instance.uuid(), e.getMessage());
            }
        }
        
        log.info("Cancelled {} future sessions for class definition: {}", cancelledCount, classDefinitionUuid);
        return cancelledCount;
    }

    @Override
    public void validateClassDefinitionForScheduling(ClassDefinitionDTO classDefinition) {
        if (classDefinition == null) {
            throw new IllegalArgumentException("Class definition cannot be null");
        }
        if (classDefinition.defaultInstructorUuid() == null) {
            throw new IllegalArgumentException("Class definition must have a default instructor");
        }
        if (classDefinition.defaultStartTime() == null) {
            throw new IllegalArgumentException("Class definition must have a default start time");
        }
        if (classDefinition.defaultEndTime() == null) {
            throw new IllegalArgumentException("Class definition must have a default end time");
        }
        if (!classDefinition.defaultStartTime().isBefore(classDefinition.defaultEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (!Boolean.TRUE.equals(classDefinition.isActive())) {
            throw new IllegalArgumentException("Only active class definitions can be scheduled");
        }
    }

    @Override
    public void validateRecurrencePattern(RecurrencePatternDTO recurrencePattern) {
        if (recurrencePattern == null) {
            throw new IllegalArgumentException("Recurrence pattern cannot be null");
        }
        if (recurrencePattern.recurrenceType() == null) {
            throw new IllegalArgumentException("Recurrence type is required");
        }
        
        switch (recurrencePattern.recurrenceType()) {
            case WEEKLY -> {
                if (recurrencePattern.daysOfWeek() == null || recurrencePattern.daysOfWeek().trim().isEmpty()) {
                    throw new IllegalArgumentException("Days of week must be specified for weekly recurrence");
                }
            }
            case MONTHLY -> {
                if (recurrencePattern.dayOfMonth() == null) {
                    throw new IllegalArgumentException("Day of month must be specified for monthly recurrence");
                }
                if (recurrencePattern.dayOfMonth() < 1 || recurrencePattern.dayOfMonth() > 31) {
                    throw new IllegalArgumentException("Day of month must be between 1 and 31");
                }
            }
            case DAILY -> {
                // No additional validation needed for daily recurrence
            }
        }
        
        if (recurrencePattern.intervalValue() != null && recurrencePattern.intervalValue() <= 0) {
            throw new IllegalArgumentException("Interval value must be positive");
        }
        
        if (recurrencePattern.occurrenceCount() != null && recurrencePattern.occurrenceCount() <= 0) {
            throw new IllegalArgumentException("Occurrence count must be positive");
        }
    }

    @Override
    public List<LocalDate> calculateOccurrenceDates(
            RecurrencePatternDTO recurrencePattern,
            LocalDate startDate,
            LocalDate endDate) {
        
        log.debug("Calculating occurrence dates for pattern: {} from {} to {}", 
                recurrencePattern.recurrenceType(), startDate, endDate);
        
        validateRecurrencePattern(recurrencePattern);
        
        List<LocalDate> occurrenceDates = new ArrayList<>();
        LocalDate effectiveEndDate = determineEffectiveEndDate(recurrencePattern, endDate);
        int intervalValue = recurrencePattern.intervalValue() != null ? recurrencePattern.intervalValue() : 1;
        
        switch (recurrencePattern.recurrenceType()) {
            case DAILY -> calculateDailyOccurrences(
                    occurrenceDates, startDate, effectiveEndDate, intervalValue, recurrencePattern.occurrenceCount());
            
            case WEEKLY -> calculateWeeklyOccurrences(
                    occurrenceDates, startDate, effectiveEndDate, intervalValue, 
                    recurrencePattern.daysOfWeek(), recurrencePattern.occurrenceCount());
            
            case MONTHLY -> calculateMonthlyOccurrences(
                    occurrenceDates, startDate, effectiveEndDate, intervalValue, 
                    recurrencePattern.dayOfMonth(), recurrencePattern.occurrenceCount());
        }
        
        log.debug("Calculated {} occurrence dates", occurrenceDates.size());
        return occurrenceDates;
    }

    // Private helper methods

    private LocalDate determineEffectiveEndDate(RecurrencePatternDTO pattern, LocalDate requestedEndDate) {
        LocalDate effectiveEndDate = requestedEndDate;
        
        if (pattern.endDate() != null) {
            if (requestedEndDate != null) {
                effectiveEndDate = pattern.endDate().isBefore(requestedEndDate) ? 
                        pattern.endDate() : requestedEndDate;
            } else {
                effectiveEndDate = pattern.endDate();
            }
        } else if (requestedEndDate == null) {
            // Default to 1 year from now if no end date specified
            effectiveEndDate = LocalDate.now().plusYears(1);
        }
        
        return effectiveEndDate;
    }

    private void calculateDailyOccurrences(List<LocalDate> occurrences, LocalDate startDate, 
            LocalDate endDate, int intervalDays, Integer maxCount) {
        
        LocalDate current = startDate;
        int count = 0;
        
        while (current.isBefore(endDate) || current.equals(endDate)) {
            if (maxCount != null && count >= maxCount) {
                break;
            }
            
            occurrences.add(current);
            current = current.plusDays(intervalDays);
            count++;
        }
    }

    private void calculateWeeklyOccurrences(List<LocalDate> occurrences, LocalDate startDate, 
            LocalDate endDate, int intervalWeeks, String daysOfWeek, Integer maxCount) {
        
        if (daysOfWeek == null || daysOfWeek.trim().isEmpty()) {
            return;
        }
        
        Set<DayOfWeek> targetDays = parseDaysOfWeek(daysOfWeek);
        LocalDate current = startDate;
        int count = 0;
        
        // Find the first week that contains any of our target days
        while (current.isBefore(endDate) || current.equals(endDate)) {
            if (maxCount != null && count >= maxCount) {
                break;
            }
            
            // Check each day in the current week
            LocalDate weekStart = current.with(DayOfWeek.MONDAY);
            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                LocalDate dayInWeek = weekStart.plusDays(dayOffset);
                
                if (targetDays.contains(dayInWeek.getDayOfWeek()) && 
                    !dayInWeek.isBefore(startDate) && 
                    (dayInWeek.isBefore(endDate) || dayInWeek.equals(endDate))) {
                    
                    if (maxCount != null && count >= maxCount) {
                        return;
                    }
                    
                    occurrences.add(dayInWeek);
                    count++;
                }
            }
            
            current = current.plusWeeks(intervalWeeks);
        }
    }

    private void calculateMonthlyOccurrences(List<LocalDate> occurrences, LocalDate startDate, 
            LocalDate endDate, int intervalMonths, int dayOfMonth, Integer maxCount) {
        
        LocalDate current = startDate.withDayOfMonth(1); // Start from first day of month
        int count = 0;
        
        while (current.isBefore(endDate) || current.equals(endDate)) {
            if (maxCount != null && count >= maxCount) {
                break;
            }
            
            // Calculate the target date for this month
            LocalDate targetDate = getValidDayInMonth(current.getYear(), current.getMonth(), dayOfMonth);
            
            if (!targetDate.isBefore(startDate) && 
                (targetDate.isBefore(endDate) || targetDate.equals(endDate))) {
                
                occurrences.add(targetDate);
                count++;
            }
            
            current = current.plusMonths(intervalMonths);
        }
    }

    private Set<DayOfWeek> parseDaysOfWeek(String daysOfWeek) {
        Set<DayOfWeek> days = new HashSet<>();
        String[] dayNames = daysOfWeek.toUpperCase().split(",");
        
        for (String dayName : dayNames) {
            String trimmedDay = dayName.trim();
            try {
                DayOfWeek day = DayOfWeek.valueOf(trimmedDay);
                days.add(day);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid day of week: {}", trimmedDay);
            }
        }
        
        return days;
    }

    private LocalDate getValidDayInMonth(int year, java.time.Month month, int dayOfMonth) {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        int lastDayOfMonth = firstOfMonth.lengthOfMonth();
        
        // If requested day doesn't exist in this month, use the last day of the month
        int validDay = Math.min(dayOfMonth, lastDayOfMonth);
        return LocalDate.of(year, month, validDay);
    }

    private boolean hasSchedulingConflict(ScheduleRequestDTO scheduleRequest) {
        return timetableService.hasInstructorConflict(
                scheduleRequest.instructorUuid(),
                scheduleRequest
        );
    }
}
