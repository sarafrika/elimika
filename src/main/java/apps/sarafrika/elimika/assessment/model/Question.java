package apps.sarafrika.elimika.assessment.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "question")
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
