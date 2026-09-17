package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.TrainingRateUpdateStatusConverter;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** An approved applicant's proposed replacement rate card (the full card, not a diff), held until the owner decides. */
@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class TrainingRateUpdate extends BaseEntity implements TrainingRateCardHolder {

    @Column(name = "application_uuid")
    private UUID applicationUuid;

    @Column(name = "rate_currency")
    private String rateCurrency;

    @Column(name = "private_online_hourly_rate")
    private BigDecimal privateOnlineHourlyRate;

    @Column(name = "private_inperson_hourly_rate")
    private BigDecimal privateInpersonHourlyRate;

    @Column(name = "group_online_hourly_rate")
    private BigDecimal groupOnlineHourlyRate;

    @Column(name = "group_inperson_hourly_rate")
    private BigDecimal groupInpersonHourlyRate;

    @Column(name = "private_online_session_rate")
    private BigDecimal privateOnlineSessionRate;

    @Column(name = "private_inperson_session_rate")
    private BigDecimal privateInpersonSessionRate;

    @Column(name = "group_online_session_rate")
    private BigDecimal groupOnlineSessionRate;

    @Column(name = "group_inperson_session_rate")
    private BigDecimal groupInpersonSessionRate;

    @Column(name = "private_online_daily_rate")
    private BigDecimal privateOnlineDailyRate;

    @Column(name = "private_inperson_daily_rate")
    private BigDecimal privateInpersonDailyRate;

    @Column(name = "group_online_daily_rate")
    private BigDecimal groupOnlineDailyRate;

    @Column(name = "group_inperson_daily_rate")
    private BigDecimal groupInpersonDailyRate;

    @Column(name = "note")
    private String note;

    @Column(name = "status")
    @Convert(converter = TrainingRateUpdateStatusConverter.class)
    private TrainingRateUpdateStatus status;

    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
