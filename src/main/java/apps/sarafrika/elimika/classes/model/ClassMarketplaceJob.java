package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.classes.util.converter.ClassMarketplaceJobStatusConverter;
import apps.sarafrika.elimika.classes.util.converter.ClassVisibilityConverter;
import apps.sarafrika.elimika.classes.util.converter.LocationTypeConverter;
import apps.sarafrika.elimika.classes.util.converter.SessionFormatConverter;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_marketplace_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassMarketplaceJob extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Convert(converter = ClassMarketplaceJobStatusConverter.class)
    @Column(name = "status")
    private ClassMarketplaceJobStatus status;

    @Convert(converter = ClassVisibilityConverter.class)
    @Column(name = "class_visibility")
    private ClassVisibility classVisibility;

    @Convert(converter = SessionFormatConverter.class)
    @Column(name = "session_format")
    private SessionFormat sessionFormat;

    @Column(name = "default_start_time")
    private LocalDateTime defaultStartTime;

    @Column(name = "default_end_time")
    private LocalDateTime defaultEndTime;

    @Column(name = "academic_period_start_date")
    private LocalDate academicPeriodStartDate;

    @Column(name = "academic_period_end_date")
    private LocalDate academicPeriodEndDate;

    @Column(name = "registration_period_start_date")
    private LocalDate registrationPeriodStartDate;

    @Column(name = "registration_period_end_date")
    private LocalDate registrationPeriodEndDate;

    @Column(name = "class_reminder_minutes")
    private Integer classReminderMinutes;

    @Column(name = "class_color")
    private String classColor;

    @Convert(converter = LocationTypeConverter.class)
    @Column(name = "location_type")
    private LocationType locationType;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "location_latitude")
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude")
    private BigDecimal locationLongitude;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "allow_waitlist")
    private Boolean allowWaitlist;

    @Column(name = "assigned_instructor_uuid")
    private UUID assignedInstructorUuid;

    @Column(name = "assigned_application_uuid")
    private UUID assignedApplicationUuid;

    @Column(name = "assigned_class_definition_uuid")
    private UUID assignedClassDefinitionUuid;

    @Column(name = "filled_at")
    private LocalDateTime filledAt;
}
