package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_assessment_scores")
public class CourseAssessmentScore extends BaseEntity {

    @Column(name = "enrollment_uuid")
    private UUID enrollmentUuid;

    @Column(name = "assessment_uuid")
    private UUID assessmentUuid;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "graded_by_uuid")
    private UUID gradedByUuid;

    @Column(name = "comments")
    private String comments;
}