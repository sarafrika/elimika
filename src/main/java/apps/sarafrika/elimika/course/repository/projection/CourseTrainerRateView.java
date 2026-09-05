package apps.sarafrika.elimika.course.repository.projection;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The same directory row as {@link CourseTrainerView}, plus the rate card — loaded only for the
 * course owner and platform administrators.
 * <p>
 * Kept as a separate projection rather than as an optional tail on the unprivileged one so that the
 * decision "may this caller see rates?" is taken once, before any query runs, and is visible in the
 * choice of repository method rather than buried in a mapping step. There is no code path on which
 * an unprivileged caller executes the query that selects these columns.
 */
public record CourseTrainerRateView(CourseTrainingApplicantType applicantType,
                                    UUID applicantUuid,
                                    LocalDateTime approvedAt,
                                    String rateCurrency,
                                    BigDecimal privateOnlineHourlyRate,
                                    BigDecimal privateInpersonHourlyRate,
                                    BigDecimal groupOnlineHourlyRate,
                                    BigDecimal groupInpersonHourlyRate,
                                    BigDecimal privateOnlineSessionRate,
                                    BigDecimal privateInpersonSessionRate,
                                    BigDecimal groupOnlineSessionRate,
                                    BigDecimal groupInpersonSessionRate,
                                    BigDecimal privateOnlineDailyRate,
                                    BigDecimal privateInpersonDailyRate,
                                    BigDecimal groupOnlineDailyRate,
                                    BigDecimal groupInpersonDailyRate) {

    /** The identity half of the row, so callers assemble both shapes through one code path. */
    public CourseTrainerView identity() {
        return new CourseTrainerView(applicantType, applicantUuid, approvedAt);
    }
}
