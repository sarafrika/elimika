package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.resourcing.spi.InstanceWindow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The instructor twin of {@code ResourceBookingService}: the hold-firm-confirm-release lifecycle
 * a venue gets, applied to the one resource a marketplace job cannot double-book. Mutations run
 * inside the caller's transaction so hold state stays atomic with job and application state.
 */
public interface InstructorTimeHoldService {

    /**
     * Places TENTATIVE holds on every window, replacing any the application already has. TENTATIVE
     * never blocks: an instructor may apply to overlapping jobs, and only a hire is exclusive.
     */
    void holdForApplication(InstructorTimeHoldRequest request);

    /**
     * Promotes the application's TENTATIVE holds to FIRM on hire. Idempotent.
     */
    void firmHoldsForApplication(UUID applicationUuid);

    /**
     * Reserves the hired diary whether or not it already holds anything, because an application
     * predating this table holds nothing and an unheld hire is one a rival can book over.
     */
    void firmOrCreateHoldsForApplication(InstructorTimeHoldRequest request);

    /**
     * Releases every active hold belonging to the application (rejected, withdrawn, superseded).
     * Idempotent; no-op when the application holds nothing.
     */
    void releaseHoldsForApplication(UUID applicationUuid, String reason);

    /**
     * Releases every active hold belonging to the job (cancelled, expired, rescheduled).
     * Idempotent.
     */
    void releaseHoldsForJob(UUID jobUuid, String reason);

    /**
     * Releases every active hold on the job except the hired application's, so losing applicants
     * get their diary back at once. A null keepApplicationUuid releases all of them.
     */
    void releaseHoldsForJobExcept(UUID jobUuid, UUID keepApplicationUuid, String reason);

    /**
     * Converts the job's active holds to CONFIRMED, linking each to its scheduled instance by exact
     * window match. A hold whose window was never scheduled is released instead.
     */
    void confirmHoldsForJob(UUID jobUuid, UUID classDefinitionUuid, List<InstanceWindow> instanceWindows);

    /**
     * Holds to draw on an instructor's calendar: TENTATIVE and FIRM only. Deliberately a separate
     * read, so {@code getScheduleForInstructor} keeps returning real sessions and nothing else.
     */
    List<InstructorTimeHoldDTO> findActiveHolds(UUID instructorUuid, LocalDate start, LocalDate end);

    /**
     * Holds that count as a clash: FIRM only, never TENTATIVE. Holds of excludingJobUuid are
     * ignored, so a job's own holds never block hiring for it.
     */
    List<InstructorTimeHoldDTO> findBlockingHolds(UUID instructorUuid,
                                                  LocalDateTime from,
                                                  LocalDateTime to,
                                                  UUID excludingJobUuid);
}
