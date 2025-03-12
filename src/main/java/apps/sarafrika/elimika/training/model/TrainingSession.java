package apps.sarafrika.elimika.training.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "classes")
@Getter
@Setter @AllArgsConstructor @NoArgsConstructor
public class TrainingSession extends BaseEntity {
    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "trainer_uuid")
    private UUID traineruuid;

    @Column(name = "start_date")
    private ZonedDateTime startDate;

    @Column(name = "end_date")
    private ZonedDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_mode")
    private ClassMode classMode;

    @Column(name="location")
    private String location;

    @Column(columnDefinition = "TEXT", name = "meeting_link")
    private String meetingLink;

    @Column(columnDefinition = "TEXT", name = "schedule")
    private String schedule;

    @Column(name = "capacity_limit")
    private int capacityLimit;

    @Column(name = "current_enrollment_count")
    private int currentEnrollmentCount = 0;

    @Column(name = "waiting_list_count")
    private int waitingListCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_or_individual")
    private GroupType groupOrIndividual;

    public enum ClassMode {
        ONLINE, IN_PERSON
    }

    public enum GroupType {
        GROUP, INDIVIDUAL
    }
}
