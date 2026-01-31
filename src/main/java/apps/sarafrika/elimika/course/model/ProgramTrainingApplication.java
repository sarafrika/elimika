package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an instructor or organisation application to deliver a training program.
 */
@Getter
@Setter
@Entity
@Table(name = "program_training_applications")
@NoArgsConstructor
@AllArgsConstructor
public class ProgramTrainingApplication extends BaseEntity {

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "applicant_type")
    @Convert(converter = apps.sarafrika.elimika.course.util.converter.CourseTrainingApplicantTypeConverter.class)
    private CourseTrainingApplicantType applicantType;

    @Column(name = "applicant_uuid")
    private UUID applicantUuid;

    @Column(name = "rate_currency")
    private String rateCurrency;

    @Column(name = "private_online_rate")
    private BigDecimal privateOnlineRate;

    @Column(name = "private_inperson_rate")
    private BigDecimal privateInpersonRate;

    @Column(name = "group_online_rate")
    private BigDecimal groupOnlineRate;

    @Column(name = "group_inperson_rate")
    private BigDecimal groupInpersonRate;

    @Column(name = "status")
    @Convert(converter = apps.sarafrika.elimika.course.util.converter.CourseTrainingApplicationStatusConverter.class)
    private CourseTrainingApplicationStatus status;

    @Column(name = "application_notes")
    private String applicationNotes;

    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
