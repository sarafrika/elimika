package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Assessment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    private String description;

    @Column(nullable = false)
    private int maximumScore;

    @Column(nullable = false)
    private int passingScore;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dueDate;

    @Column(nullable = false)
    private int timeLimit;

    @Column(nullable = false)
    private Long courseId;

    private Long lessonId;
}
