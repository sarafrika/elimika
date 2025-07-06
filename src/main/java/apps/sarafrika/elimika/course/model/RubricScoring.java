package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
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
    private UUID criteriaUuid;

    @Column(name = "grading_level_uuid")
    private UUID gradingLevelUuid;

    @Column(name = "description")
    private String description;
}