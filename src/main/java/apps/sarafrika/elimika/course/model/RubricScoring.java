package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rubric_scoring")
public class RubricScoring extends BaseEntity {

    @Column(name = "criteria_uuid")
    @Filterable
    private UUID criteriaUuid;

    @Column(name = "rubric_scoring_level_uuid")
    @Filterable
    private UUID rubricScoringLevelUuid;

    @Column(name = "description")
    private String description;
}