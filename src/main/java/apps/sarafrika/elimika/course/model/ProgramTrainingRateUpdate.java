package apps.sarafrika.elimika.course.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

/** A proposed rate change on an approved program training application. */
@Entity
@Table(name = "program_training_rate_updates")
@NoArgsConstructor
public class ProgramTrainingRateUpdate extends TrainingRateUpdate {
}
