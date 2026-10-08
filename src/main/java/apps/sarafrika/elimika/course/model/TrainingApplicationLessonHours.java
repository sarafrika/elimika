package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** How many hours one learner group spends on one lesson. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "training_application_lesson_hours")
public class TrainingApplicationLessonHours extends BaseEntity {

    @Column(name = "learner_group_uuid")
    private UUID learnerGroupUuid;

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "hours")
    private BigDecimal hours;
}
