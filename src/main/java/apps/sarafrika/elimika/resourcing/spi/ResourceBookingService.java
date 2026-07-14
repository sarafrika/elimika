package apps.sarafrika.elimika.resourcing.spi;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking operations on organisation resources, exposed to the classes and
 * timetabling modules.
 * <p>
 * All mutating operations are intended to be called synchronously inside the
 * caller's transaction so booking state stays atomic with job/class state.
 */
public interface ResourceBookingService {

    /**
     * Validates a set of booking requests without reserving anything.
     *
     * @param organisationUuid organisation the resources must belong to
     * @param requests         resources and windows to validate
     * @return per-occurrence conflict report; {@code clean} when everything fits
     */
    ResourceValidationReport validateBookings(UUID organisationUuid, List<ResourceBookingRequest> requests);

    /**
     * Atomically validates every requested window and creates HOLD bookings tied to
     * the marketplace job. Serialises against concurrent bookings on the same
     * resources via pessimistic resource-row locks.
     *
     * @throws ResourceBookingConflictException when any window conflicts; carries the full report
     */
    void holdResourcesForJob(UUID jobUuid, UUID organisationUuid, List<ResourceBookingRequest> requests);

    /**
     * Releases every active HOLD belonging to the job (job cancelled, expired or updated).
     * Idempotent; no-op when the job has no active holds.
     */
    void releaseHoldsForJob(UUID jobUuid, String reason);

    /**
     * Converts the job's HOLD bookings to CONFIRMED, stamping the class definition
     * and linking each booking to its scheduled instance by exact (start, end)
     * window match. Holds without a matching instance window are released.
     */
    void confirmHoldsForJob(UUID jobUuid, UUID classDefinitionUuid, List<InstanceWindow> instanceWindows);

    /**
     * Conflict check for a single window on a single resource.
     *
     * @param excludeJobUuid             ignore holds belonging to this job (a job's own holds
     *                                   must not block the class created from it)
     * @param excludeClassDefinitionUuid ignore bookings belonging to this class definition
     * @return conflicts for the window; empty when the window fits
     */
    List<ResourceConflictDetail> findConflicts(UUID resourceUuid,
                                               int quantity,
                                               LocalDateTime start,
                                               LocalDateTime end,
                                               UUID excludeJobUuid,
                                               UUID excludeClassDefinitionUuid);

    /**
     * Validates then creates CONFIRMED bookings for a scheduled instance added after
     * assignment (e.g. a session template appended to an existing class).
     *
     * @throws ResourceBookingConflictException when any resource cannot cover the window
     */
    void createConfirmedBookingsForInstance(UUID classDefinitionUuid,
                                            UUID scheduledInstanceUuid,
                                            LocalDateTime start,
                                            LocalDateTime end,
                                            List<ResourceBookingRequest> resources);

    /**
     * Moves every booking linked to the scheduled instance to a new window, validating
     * the new window against other bookings and availability rules first.
     *
     * @throws ResourceBookingConflictException when the new window conflicts
     */
    void rescheduleInstanceBookings(UUID scheduledInstanceUuid, LocalDateTime newStart, LocalDateTime newEnd);

    /**
     * Cancels every active booking linked to the scheduled instance. Idempotent.
     */
    void releaseBookingsForInstance(UUID scheduledInstanceUuid, String reason);
}
