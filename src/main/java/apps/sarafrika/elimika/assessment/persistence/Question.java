package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Question extends BaseEntity {

    @Column(name = "description")
    private String description;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "point_value")
    private int pointValue;

    @Column(name = "order_in_assessment")
    private int orderInAssessment;

    @Column(name = " assessment_id")
    private Long assessmentId;

}
