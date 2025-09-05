package apps.sarafrika.elimika.timetabling.dto;

import apps.sarafrika.elimika.timetabling.util.enums.EnrollmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event DTO published when attendance is marked for a student enrollment.
 * <p>
 * This event notifies other modules about attendance status changes,
 * which can be used for progress tracking, notifications, or reporting.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
public record AttendanceMarkedEventDTO(
        UUID enrollmentUuid,
        UUID instanceUuid,
        UUID studentUuid,
        UUID classDefinitionUuid,
        UUID instructorUuid,
        EnrollmentStatus attendanceStatus,
        LocalDateTime markedAt,
        String classTitle
) {}