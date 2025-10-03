package apps.sarafrika.elimika.availability.spi;

import apps.sarafrika.elimika.availability.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service Provider Interface for Availability operations.
 * <p>
 * This interface defines the public API that other modules can use to interact
 * with the Availability module. It provides operations for managing instructor
 * availability patterns including daily, weekly, monthly, and custom patterns.
 * <p>
 * The SPI follows Spring Modulith patterns for inter-module communication
 * and maintains clean boundaries between modules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public interface AvailabilityService {

    /**
     * Sets weekly availability patterns for an instructor.
     * This will replace any existing weekly availability for the instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @param slots List of weekly availability slots to set
     * @throws IllegalArgumentException if instructorUuid is null or slots is empty
     */
    void setWeeklyAvailability(UUID instructorUuid, List<WeeklyAvailabilitySlotDTO> slots);

    /**
     * Sets daily availability patterns for an instructor.
     * This will replace any existing daily availability for the instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @param slots List of daily availability slots to set
     * @throws IllegalArgumentException if instructorUuid is null or slots is empty
     */
    void setDailyAvailability(UUID instructorUuid, List<DailyAvailabilitySlotDTO> slots);

    /**
     * Sets monthly availability patterns for an instructor.
     * This will replace any existing monthly availability for the instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @param slots List of monthly availability slots to set
     * @throws IllegalArgumentException if instructorUuid is null or slots is empty
     */
    void setMonthlyAvailability(UUID instructorUuid, List<MonthlyAvailabilitySlotDTO> slots);

    /**
     * Sets custom availability patterns for an instructor.
     * This will replace any existing custom availability for the instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @param slots List of custom availability slots to set
     * @throws IllegalArgumentException if instructorUuid is null or slots is empty
     */
    void setCustomAvailability(UUID instructorUuid, List<CustomAvailabilitySlotDTO> slots);

    /**
     * Creates a single availability slot.
     *
     * @param slot The availability slot to create
     * @return The created availability slot with generated UUID and audit fields
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    AvailabilitySlotDTO createAvailabilitySlot(AvailabilitySlotDTO slot);

    /**
     * Updates an existing availability slot.
     *
     * @param slotUuid The UUID of the availability slot to update
     * @param slot The updated availability slot data
     * @return The updated availability slot
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if availability slot is not found
     */
    AvailabilitySlotDTO updateAvailabilitySlot(UUID slotUuid, AvailabilitySlotDTO slot);

    /**
     * Deletes an availability slot.
     *
     * @param slotUuid The UUID of the availability slot to delete
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if availability slot is not found
     */
    void deleteAvailabilitySlot(UUID slotUuid);

    /**
     * Retrieves an availability slot by its UUID.
     *
     * @param slotUuid The UUID of the availability slot to retrieve
     * @return The availability slot if found
     * @throws IllegalArgumentException if the UUID is null or invalid
     * @throws RuntimeException if availability slot is not found
     */
    AvailabilitySlotDTO getAvailabilitySlot(UUID slotUuid);

    /**
     * Retrieves all availability slots for a specific instructor.
     *
     * @param instructorUuid The UUID of the instructor
     * @return List of availability slots for the instructor (empty list if none found)
     * @throws IllegalArgumentException if the UUID is null or invalid
     */
    List<AvailabilitySlotDTO> getAvailabilityForInstructor(UUID instructorUuid);

    /**
     * Retrieves availability slots for an instructor on a specific date.
     * This considers all applicable patterns (daily, weekly, monthly, custom)
     * and returns the effective availability for that date.
     *
     * @param instructorUuid The UUID of the instructor
     * @param date The date to check availability for
     * @return List of availability slots for the date (empty list if none found)
     * @throws IllegalArgumentException if instructorUuid or date is null
     */
    List<AvailabilitySlotDTO> getAvailabilityForDate(UUID instructorUuid, LocalDate date);

    /**
     * Checks if an instructor is available during a specific time period.
     * This method considers all availability patterns and blocked times.
     *
     * @param instructorUuid The UUID of the instructor
     * @param start The start date/time to check
     * @param end The end date/time to check
     * @return true if the instructor is available for the entire period, false otherwise
     * @throws IllegalArgumentException if any parameter is null or if start is after end
     */
    boolean isInstructorAvailable(UUID instructorUuid, LocalDateTime start, LocalDateTime end);

    /**
     * Finds available time slots for an instructor within a date range.
     * Returns only slots where isAvailable = true.
     * Useful for scheduling systems that need to find available periods for booking classes.
     *
     * @param instructorUuid The UUID of the instructor
     * @param startDate The start date of the range to search
     * @param endDate The end date of the range to search
     * @return List of available slots within the date range (empty list if none found)
     * @throws IllegalArgumentException if any parameter is null or if startDate is after endDate
     */
    List<AvailabilitySlotDTO> findAvailableSlots(UUID instructorUuid, LocalDate startDate, LocalDate endDate);

    /**
     * Clears all availability patterns for an instructor.
     * This is useful when completely resetting an instructor's availability.
     *
     * @param instructorUuid The UUID of the instructor
     * @throws IllegalArgumentException if the UUID is null or invalid
     */
    void clearAvailability(UUID instructorUuid);

    /**
     * Blocks time for an instructor during a specific period.
     * This creates availability slots with isAvailable = false.
     * Optionally accepts a color code for visual categorization.
     *
     * @param instructorUuid The UUID of the instructor
     * @param start The start date/time to block
     * @param end The end date/time to block
     * @param colorCode Optional hex color code for UI visualization (e.g., "#FF6B6B")
     * @throws IllegalArgumentException if any parameter is null or if start is after end
     */
    void blockTime(UUID instructorUuid, LocalDateTime start, LocalDateTime end, String colorCode);
}