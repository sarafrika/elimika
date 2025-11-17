package apps.sarafrika.elimika.availability.service.impl;

import apps.sarafrika.elimika.availability.dto.*;
import apps.sarafrika.elimika.availability.factory.AvailabilityFactory;
import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.availability.repository.AvailabilityRepository;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GenericSpecificationBuilder<InstructorAvailability> specificationBuilder;

    private static final String AVAILABILITY_SLOT_NOT_FOUND_TEMPLATE = "Availability slot with UUID %s not found";

    @Override
    public void setWeeklyAvailability(UUID instructorUuid, List<WeeklyAvailabilitySlotDTO> slots) {
        log.debug("Setting weekly availability for instructor: {}", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Weekly availability slots cannot be null or empty");
        }

        // Remove existing weekly availability
        List<InstructorAvailability> existingWeeklySlots = 
            availabilityRepository.findByInstructorUuidAndAvailabilityType(instructorUuid, AvailabilityType.WEEKLY);
        availabilityRepository.deleteAll(existingWeeklySlots);

        // Create new weekly availability slots
        List<InstructorAvailability> entities = slots.stream()
            .map(AvailabilityFactory::toEntity)
            .collect(Collectors.toList());
        
        availabilityRepository.saveAll(entities);
        log.debug("Created {} weekly availability slots for instructor: {}", entities.size(), instructorUuid);
    }

    @Override
    public void setDailyAvailability(UUID instructorUuid, List<DailyAvailabilitySlotDTO> slots) {
        log.debug("Setting daily availability for instructor: {}", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Daily availability slots cannot be null or empty");
        }

        // Remove existing daily availability
        List<InstructorAvailability> existingDailySlots = 
            availabilityRepository.findByInstructorUuidAndAvailabilityType(instructorUuid, AvailabilityType.DAILY);
        availabilityRepository.deleteAll(existingDailySlots);

        // Create new daily availability slots
        List<InstructorAvailability> entities = slots.stream()
            .map(AvailabilityFactory::toEntity)
            .collect(Collectors.toList());
        
        availabilityRepository.saveAll(entities);
        log.debug("Created {} daily availability slots for instructor: {}", entities.size(), instructorUuid);
    }

    @Override
    public void setMonthlyAvailability(UUID instructorUuid, List<MonthlyAvailabilitySlotDTO> slots) {
        log.debug("Setting monthly availability for instructor: {}", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Monthly availability slots cannot be null or empty");
        }

        // Remove existing monthly availability
        List<InstructorAvailability> existingMonthlySlots = 
            availabilityRepository.findByInstructorUuidAndAvailabilityType(instructorUuid, AvailabilityType.MONTHLY);
        availabilityRepository.deleteAll(existingMonthlySlots);

        // Create new monthly availability slots
        List<InstructorAvailability> entities = slots.stream()
            .map(AvailabilityFactory::toEntity)
            .collect(Collectors.toList());
        
        availabilityRepository.saveAll(entities);
        log.debug("Created {} monthly availability slots for instructor: {}", entities.size(), instructorUuid);
    }

    @Override
    public void setCustomAvailability(UUID instructorUuid, List<CustomAvailabilitySlotDTO> slots) {
        log.debug("Setting custom availability for instructor: {}", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Custom availability slots cannot be null or empty");
        }

        // Remove existing custom availability
        List<InstructorAvailability> existingCustomSlots = 
            availabilityRepository.findByInstructorUuidAndAvailabilityType(instructorUuid, AvailabilityType.CUSTOM);
        availabilityRepository.deleteAll(existingCustomSlots);

        // Create new custom availability slots
        List<InstructorAvailability> entities = slots.stream()
            .map(AvailabilityFactory::toEntity)
            .collect(Collectors.toList());
        
        availabilityRepository.saveAll(entities);
        log.debug("Created {} custom availability slots for instructor: {}", entities.size(), instructorUuid);
    }

    @Override
    public AvailabilitySlotDTO createAvailabilitySlot(AvailabilitySlotDTO slot) {
        log.debug("Creating availability slot for instructor: {}", slot.instructorUuid());
        
        if (slot == null) {
            throw new IllegalArgumentException("Availability slot cannot be null");
        }

        InstructorAvailability entity = AvailabilityFactory.toEntity(slot);
        
        // Set defaults
        if (entity.getIsAvailable() == null) {
            entity.setIsAvailable(true);
        }
        if (entity.getRecurrenceInterval() == null) {
            entity.setRecurrenceInterval(1);
        }
        
        InstructorAvailability savedEntity = availabilityRepository.save(entity);
        
        log.debug("Created availability slot with UUID: {}", savedEntity.getUuid());
        return AvailabilityFactory.toDTO(savedEntity);
    }

    @Override
    public AvailabilitySlotDTO updateAvailabilitySlot(UUID slotUuid, AvailabilitySlotDTO slot) {
        log.debug("Updating availability slot: {}", slotUuid);
        
        if (slotUuid == null) {
            throw new IllegalArgumentException("Slot UUID cannot be null");
        }
        if (slot == null) {
            throw new IllegalArgumentException("Availability slot cannot be null");
        }

        InstructorAvailability entity = availabilityRepository.findByUuid(slotUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(AVAILABILITY_SLOT_NOT_FOUND_TEMPLATE, slotUuid)));

        AvailabilityFactory.updateEntityFromDTO(entity, slot);
        InstructorAvailability savedEntity = availabilityRepository.save(entity);
        
        log.debug("Updated availability slot: {}", slotUuid);
        return AvailabilityFactory.toDTO(savedEntity);
    }

    @Override
    public void deleteAvailabilitySlot(UUID slotUuid) {
        log.debug("Deleting availability slot: {}", slotUuid);
        
        if (slotUuid == null) {
            throw new IllegalArgumentException("Slot UUID cannot be null");
        }

        InstructorAvailability entity = availabilityRepository.findByUuid(slotUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(AVAILABILITY_SLOT_NOT_FOUND_TEMPLATE, slotUuid)));

        availabilityRepository.delete(entity);
        log.debug("Deleted availability slot: {}", slotUuid);
    }

    @Override
    public AvailabilitySlotDTO getAvailabilitySlot(UUID slotUuid) {
        log.debug("Getting availability slot: {}", slotUuid);

        if (slotUuid == null) {
            throw new IllegalArgumentException("Slot UUID cannot be null");
        }

        InstructorAvailability entity = availabilityRepository.findByUuid(slotUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(AVAILABILITY_SLOT_NOT_FOUND_TEMPLATE, slotUuid)));

        return AvailabilityFactory.toDTO(entity);
    }

    @Override
    public Page<AvailabilitySlotDTO> search(Map<String, String> searchParams, Pageable pageable) {
        log.debug("Searching availability with params: {}", searchParams);

        Specification<InstructorAvailability> spec = specificationBuilder.buildSpecification(InstructorAvailability.class, searchParams);
        Page<InstructorAvailability> entities = availabilityRepository.findAll(spec, pageable);

        return entities.map(AvailabilityFactory::toDTO);
    }

    @Override
    public List<AvailabilitySlotDTO> getAvailabilityForInstructor(UUID instructorUuid) {
        log.debug("Getting all availability for instructor: {}", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }

        List<InstructorAvailability> entities = availabilityRepository.findByInstructorUuid(instructorUuid);
        return AvailabilityFactory.toDTOList(entities);
    }

    @Override
    public List<AvailabilitySlotDTO> getAvailabilityForDate(UUID instructorUuid, LocalDate date) {
        log.debug("Getting availability for instructor: {} on date: {}", instructorUuid, date);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        // Get effective availability for the date
        List<InstructorAvailability> effectiveSlots = 
            availabilityRepository.findEffectiveAvailabilityForDate(instructorUuid, date);
        
        // Filter by patterns that match the date
        List<InstructorAvailability> matchingSlots = effectiveSlots.stream()
            .filter(slot -> matchesDate(slot, date))
            .collect(Collectors.toList());

        return AvailabilityFactory.toDTOList(matchingSlots);
    }

    @Override
    public boolean isInstructorAvailable(UUID instructorUuid, LocalDateTime start, LocalDateTime end) {
        log.debug("Checking if instructor: {} is available from {} to {}", instructorUuid, start, end);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        LocalDate date = start.toLocalDate();
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        // Get overlapping slots for the time range
        List<InstructorAvailability> overlappingSlots = 
            availabilityRepository.findOverlappingAvailability(instructorUuid, startTime, endTime, date);

        // Filter by patterns that match the date
        List<InstructorAvailability> matchingSlots = overlappingSlots.stream()
            .filter(slot -> matchesDate(slot, date))
            .collect(Collectors.toList());

        // Check if all matching slots are available (not blocked)
        return matchingSlots.stream()
            .allMatch(slot -> Boolean.TRUE.equals(slot.getIsAvailable()));
    }

    @Override
    public List<AvailabilitySlotDTO> findAvailableSlots(UUID instructorUuid, LocalDate startDate, LocalDate endDate) {
        log.debug("Finding available slots for instructor: {} from {} to {}", instructorUuid, startDate, endDate);

        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        // Iterate through each date in the range and collect available slots
        return startDate.datesUntil(endDate.plusDays(1))
            .flatMap(date -> getAvailabilityForDate(instructorUuid, date).stream())
            .filter(slot -> Boolean.TRUE.equals(slot.isAvailable()))
            .collect(Collectors.toList());
    }

    @Override
    public void clearAvailability(UUID instructorUuid) {
        log.debug("Clearing all availability for instructor: {}", instructorUuid);
        
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }

        List<InstructorAvailability> allSlots = availabilityRepository.findByInstructorUuid(instructorUuid);
        availabilityRepository.deleteAll(allSlots);
        
        log.debug("Cleared {} availability slots for instructor: {}", allSlots.size(), instructorUuid);
    }

    @Override
    public void blockTime(UUID instructorUuid, LocalDateTime start, LocalDateTime end, String colorCode) {
        log.debug("Blocking time for instructor: {} from {} to {} with color: {}",
                instructorUuid, start, end, colorCode);

        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        InstructorAvailability blockSlot = new InstructorAvailability();
        blockSlot.setInstructorUuid(instructorUuid);
        blockSlot.setAvailabilityType(AvailabilityType.CUSTOM);
        blockSlot.setSpecificDate(start.toLocalDate());
        blockSlot.setStartTime(start.toLocalTime());
        blockSlot.setEndTime(end.toLocalTime());
        blockSlot.setIsAvailable(false);
        blockSlot.setCustomPattern("BLOCKED_TIME_SLOT");
        blockSlot.setColorCode(colorCode);

        availabilityRepository.save(blockSlot);
        log.debug("Created blocked time slot for instructor: {} with color: {}", instructorUuid, colorCode);
    }

    @Override
    public void bookInstructorSlot(apps.sarafrika.elimika.availability.dto.InstructorSlotBookingRequestDTO request) {
        UUID instructorUuid = request.instructorUuid();
        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime();

        log.debug("Booking instructor slot for instructor: {} from {} to {} (student: {}, purpose: {})",
                instructorUuid, start, end, request.studentUuid(), request.purpose());

        if (!isInstructorAvailable(instructorUuid, start, end)) {
            throw new IllegalStateException("Instructor is not available for the requested time range.");
        }

        // For now, booking is represented by a blocked availability slot.
        // Future iterations can introduce a dedicated bookings table with richer metadata.
        blockTime(instructorUuid, start, end, "#95E1D3");
    }

    private boolean matchesDate(InstructorAvailability slot, LocalDate date) {
        return switch (slot.getAvailabilityType()) {
            case DAILY -> true; // Daily patterns always match
            case WEEKLY -> matchesWeeklyPattern(slot, date);
            case MONTHLY -> matchesMonthlyPattern(slot, date);
            case CUSTOM -> matchesCustomPattern(slot, date);
        };
    }

    private boolean matchesWeeklyPattern(InstructorAvailability slot, LocalDate date) {
        if (slot.getDayOfWeek() == null) {
            return false;
        }
        
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayNumber = dayOfWeek.getValue(); // 1 = Monday, 7 = Sunday
        
        return slot.getDayOfWeek().equals(dayNumber);
    }

    private boolean matchesMonthlyPattern(InstructorAvailability slot, LocalDate date) {
        if (slot.getDayOfMonth() == null) {
            return false;
        }
        
        return slot.getDayOfMonth().equals(date.getDayOfMonth());
    }

    private boolean matchesCustomPattern(InstructorAvailability slot, LocalDate date) {
        // For now, we'll match specific dates or assume true
        // In a full implementation, this would parse cron expressions
        if (slot.getSpecificDate() != null) {
            return slot.getSpecificDate().equals(date);
        }
        
        // For custom patterns without specific dates, we'll assume they match
        // This would need proper cron parsing in a full implementation
        return true;
    }
}
