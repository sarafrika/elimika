package apps.sarafrika.elimika.shared.spi.timetabling;

import java.util.Locale;

/**
 * Status values for instructor schedule entries exposed outside the timetabling module.
 */
public enum InstructorScheduleStatus {
    SCHEDULED,
    ONGOING,
    COMPLETED,
    CANCELLED,
    BLOCKED;

    /**
     * Parses a status value ignoring case.
     *
     * @param value the raw status value
     * @return the matching status
     */
    public static InstructorScheduleStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status value cannot be null or blank");
        }
        return InstructorScheduleStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
