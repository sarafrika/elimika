package apps.sarafrika.elimika.course.util.enums;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/** The twelve cells of a rate card, grouped into four methods (format x location); a method is offered when any basis is priced. */
public enum TrainingRateCell {
    PRIVATE_ONLINE_HOURLY("private_online_hourly_rate", "private online", CourseTrainingRateCardDTO::privateOnlineHourlyRate),
    PRIVATE_ONLINE_SESSION("private_online_session_rate", "private online", CourseTrainingRateCardDTO::privateOnlineSessionRate),
    PRIVATE_ONLINE_DAILY("private_online_daily_rate", "private online", CourseTrainingRateCardDTO::privateOnlineDailyRate),
    PRIVATE_INPERSON_HOURLY("private_inperson_hourly_rate", "private in-person", CourseTrainingRateCardDTO::privateInpersonHourlyRate),
    PRIVATE_INPERSON_SESSION("private_inperson_session_rate", "private in-person", CourseTrainingRateCardDTO::privateInpersonSessionRate),
    PRIVATE_INPERSON_DAILY("private_inperson_daily_rate", "private in-person", CourseTrainingRateCardDTO::privateInpersonDailyRate),
    GROUP_ONLINE_HOURLY("group_online_hourly_rate", "group online", CourseTrainingRateCardDTO::groupOnlineHourlyRate),
    GROUP_ONLINE_SESSION("group_online_session_rate", "group online", CourseTrainingRateCardDTO::groupOnlineSessionRate),
    GROUP_ONLINE_DAILY("group_online_daily_rate", "group online", CourseTrainingRateCardDTO::groupOnlineDailyRate),
    GROUP_INPERSON_HOURLY("group_inperson_hourly_rate", "group in-person", CourseTrainingRateCardDTO::groupInpersonHourlyRate),
    GROUP_INPERSON_SESSION("group_inperson_session_rate", "group in-person", CourseTrainingRateCardDTO::groupInpersonSessionRate),
    GROUP_INPERSON_DAILY("group_inperson_daily_rate", "group in-person", CourseTrainingRateCardDTO::groupInpersonDailyRate);

    /** The four training methods, each listed as its hourly, session and daily cell. */
    public static final List<List<TrainingRateCell>> METHODS = List.of(
            List.of(PRIVATE_ONLINE_HOURLY, PRIVATE_ONLINE_SESSION, PRIVATE_ONLINE_DAILY),
            List.of(PRIVATE_INPERSON_HOURLY, PRIVATE_INPERSON_SESSION, PRIVATE_INPERSON_DAILY),
            List.of(GROUP_ONLINE_HOURLY, GROUP_ONLINE_SESSION, GROUP_ONLINE_DAILY),
            List.of(GROUP_INPERSON_HOURLY, GROUP_INPERSON_SESSION, GROUP_INPERSON_DAILY));

    private final String fieldName;
    private final String methodLabel;
    private final Function<CourseTrainingRateCardDTO, BigDecimal> accessor;

    TrainingRateCell(String fieldName, String methodLabel, Function<CourseTrainingRateCardDTO, BigDecimal> accessor) {
        this.fieldName = fieldName;
        this.methodLabel = methodLabel;
        this.accessor = accessor;
    }

    public String fieldName() {
        return fieldName;
    }

    public String methodLabel() {
        return methodLabel;
    }

    public BigDecimal read(CourseTrainingRateCardDTO rateCard) {
        return rateCard == null ? null : accessor.apply(rateCard);
    }
}
