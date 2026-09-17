package apps.sarafrika.elimika.course.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** A proposed rate change on an approved course training application. */
@Entity
@Table(name = "course_training_rate_updates")
@NoArgsConstructor
public class CourseTrainingRateUpdate extends TrainingRateUpdate {
}
