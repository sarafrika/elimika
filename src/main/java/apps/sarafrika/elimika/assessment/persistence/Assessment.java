package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Assessment extends BaseEntity {
    @Column(name = "title")
    private String title;

    @Column(name = "type")
    private String type;

    @Column(name = "description")
    private String description;

    @Column(name = "maximum_score")
    private int maximumScore;

    @Column(name = "passing_score")
    private int passingScore;

    @Column(name = "due_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dueDate;

    @Column(name = "time_limit")
    private int timeLimit;

    @Column(name = "Course_id")
    private Long courseId;

    @Column(name = "lesson_id")
    private Long lessonId;
}
