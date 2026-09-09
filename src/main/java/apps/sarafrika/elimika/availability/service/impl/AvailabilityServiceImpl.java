package apps.sarafrika.elimika.availability.service.impl;

import apps.sarafrika.elimika.availability.dto.AvailabilitySlotDTO;
import apps.sarafrika.elimika.availability.factory.AvailabilityFactory;
import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.availability.repository.AvailabilityRepository;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.availability.util.AvailabilityTimezones;
import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.event.availability.InstructorAvailabilityChangedEventDTO;
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
import java.time.ZoneId;
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
    public AvailabilitySlotDTO createAvailabilitySlot(AvailabilitySlotDTO slot) {
        log.debug("Creating availability slot for instructor: {}", slot.instructorUuid());
        
        if (slot == null) {
            throw new IllegalArgumentException("Availability slot cannot be null");
        }

        InstructorAvailability entity = AvailabilityFactory.toEntity(slot);
        entity.setTimezone(AvailabilityTimezones.normalize(entity.getTimezone()));

        // Set defaults
        if (entity.getIsAvailable() == null) {
            entity.setIsAvailable(true);
        }
        if (entity.getRecurrenceInterval() == null) {
            entity.setRecurrenceInterval(1);
        }
        
        InstructorAvailability savedEntity = availabilityRepository.save(entity);
        
        log.debug("Created availability slot with UUID: {}", savedEntity.getUuid());
        publishAvailabilityChanged(savedEntity.getInstructorUuid(), savedEntity.getAvailabilityType(),
                resolveEffectiveDate(savedEntity), "Availability slot created");
        return AvailabilityFactory.toDTO(savedEntity);
    }

    @Override
    public AvailabilitySlotDTO updateAvailabilitySlot(UUID instructorUuid, UUID slotUuid, AvailabilitySlotDTO slot) {
        log.debug("Updating availability slot {} for instructor {}", slotUuid, instructorUuid);

        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (slotUuid == null) {
            throw new IllegalArgumentException("Slot UUID cannot be null");
        }
        if (slot == null) {
            throw new IllegalArgumentException("Availability slot cannot be null");
        }

        InstructorAvailability entity = findOwnedSlot(instructorUuid, slotUuid);

        AvailabilityFactory.updateEntityFromDTO(entity, slot);
        entity.setTimezone(AvailabilityTimezones.normalize(entity.getTimezone()));
        InstructorAvailability savedEntity = availabilityRepository.save(entity);
        
        log.debug("Updated availability slot: {}", slotUuid);
        publishAvailabilityChanged(savedEntity.getInstructorUuid(), savedEntity.getAvailabilityType(),
                resolveEffectiveDate(savedEntity), "Availability slot updated");
        return AvailabilityFactory.toDTO(savedEntity);
    }

    @Override
    public void deleteAvailabilitySlot(UUID instructorUuid, UUID slotUuid) {
        log.debug("Deleting availability slot {} for instructor {}", slotUuid, instructorUuid);

        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (slotUuid == null) {
            throw new IllegalArgumentException("Slot UUID cannot be null");
        }

        InstructorAvailability entity = findOwnedSlot(instructorUuid, slotUuid);

        availabilityRepository.delete(entity);
        log.debug("Deleted availability slot: {}", slotUuid);
        publishAvailabilityChanged(entity.getInstructorUuid(), entity.getAvailabilityType(),
                resolveEffectiveDate(entity), "Availability slot deleted");
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

        specificationBuilder.validateSortProperties(InstructorAvailability.class, pageable);
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

        // A day either side of the UTC window, because a slot's own zone can put its date on either
        // one; the window cannot be narrowed in SQL until each row's zone is in hand.
        List<InstructorAvailability> candidates = availabilityRepository.findEffectiveAvailabilityBetween(
            instructorUuid, start.toLocalDate().minusDays(1), end.toLocalDate().plusDays(1));

        // Only an explicit block ever refuses, so a window nothing covers stays open and the per-row
        // zone arithmetic is spent on the rows that could say no rather than on the whole calendar.
        return candidates.stream()
            .filter(slot -> Boolean.FALSE.equals(slot.getIsAvailable()))
            .noneMatch(slot -> coversWindow(slot, start, end));
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

    /**
     * Decides whether a slot really touches a UTC window.
     * <p>
     * The slot's start and end are a wall clock in the slot's own zone, so the window is moved into
     * that zone before the two are compared, and the comparison is run against every local date the
     * window can fall on. The overlap stays half-open, as its JPQL sibling has it: a block starting
     * at 12:00 does not cover a class ending at 12:00.
     */
    private boolean coversWindow(InstructorAvailability slot, LocalDateTime startUtc, LocalDateTime endUtc) {
        if (slot.getStartTime() == null || slot.getEndTime() == null) {
            return false;
        }

        ZoneId zone = AvailabilityTimezones.resolve(slot.getTimezone());
        LocalDateTime localStart = AvailabilityTimezones.fromUtc(startUtc, zone);
        LocalDateTime localEnd = AvailabilityTimezones.fromUtc(endUtc, zone);

        return localStart.toLocalDate().datesUntil(localEnd.toLocalDate().plusDays(1))
            .anyMatch(date -> isEffectiveOn(slot, date)
                    && matchesDate(slot, date)
                    && date.atTime(slot.getStartTime()).isBefore(localEnd)
                    && date.atTime(slot.getEndTime()).isAfter(localStart));
    }

    private boolean isEffectiveOn(InstructorAvailability slot, LocalDate date) {
        return (slot.getEffectiveStartDate() == null || !slot.getEffectiveStartDate().isAfter(date))
                && (slot.getEffectiveEndDate() == null || !slot.getEffectiveEndDate().isBefore(date));
    }

    private boolean matchesDate(InstructorAvailability slot, LocalDate date) {
        return switch (slot.getAvailabilityType()) {
            case DAILY -> true; // Daily patterns always match
            case WEEKLY -> matchesWeeklyPattern(slot, date);
            case MONTHLY -> matchesMonthlyPattern(slot, date);
            case CUSTOM -> matchesCustomPattern(slot, date);
        };
    }

    /**
     * Loads a slot that is on the given instructor's calendar, in one read.
     * <p>
     * A slot belonging to a different instructor is reported as missing rather than as forbidden:
     * the caller is entitled to know about their own calendar and nothing else, so which of somebody
     * else's slot UUIDs happen to be real is not an answer this should give.
     */
    private InstructorAvailability findOwnedSlot(UUID instructorUuid, UUID slotUuid) {
        return availabilityRepository.findByUuid(slotUuid)
                .filter(entity -> instructorUuid.equals(entity.getInstructorUuid()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(AVAILABILITY_SLOT_NOT_FOUND_TEMPLATE, slotUuid)));
    }

    private void publishAvailabilityChanged(UUID instructorUuid, AvailabilityType type, LocalDate effectiveDate, String description) {
        if (instructorUuid == null) {
            return;
        }
        AvailabilityType safeType = type != null ? type : AvailabilityType.CUSTOM;
        LocalDate effective = effectiveDate != null ? effectiveDate : LocalDate.now();
        InstructorAvailabilityChangedEventDTO event = new InstructorAvailabilityChangedEventDTO(
                instructorUuid,
                safeType,
                effective,
                description
        );
        eventPublisher.publishEvent(event);
    }

    private LocalDate resolveEffectiveDate(InstructorAvailability availability) {
        if (availability.getSpecificDate() != null) {
            return availability.getSpecificDate();
        }
        if (availability.getEffectiveStartDate() != null) {
            return availability.getEffectiveStartDate();
        }
        return LocalDate.now();
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
