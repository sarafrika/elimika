package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Question extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Column(nullable = false)
    private String questionType;

    @Column(nullable = false)
    private int pointValue;

    @Column(nullable = false)
    private int orderInAssessment;

    @Column(nullable = false)
    private Long assessmentId;

}
