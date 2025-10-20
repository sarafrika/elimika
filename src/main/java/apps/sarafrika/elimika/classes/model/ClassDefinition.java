package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "class_definitions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClassDefinition extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "default_instructor_uuid")
    private UUID defaultInstructorUuid;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "default_start_time")
    private LocalTime defaultStartTime;

    @Column(name = "default_end_time")
    private LocalTime defaultEndTime;

    @Column(name = "location_type")
    private LocationType locationType;

    @Column(name = "max_participants")
    private Integer maxParticipants = 50;

    @Column(name = "allow_waitlist")
    private Boolean allowWaitlist = true;

    @Column(name = "recurrence_pattern_uuid")
    private UUID recurrencePatternUuid;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "training_fee")
    private BigDecimal trainingFee;

}
