package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.AttemptStatusConverter;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_attempts")
public class QuizAttempt extends BaseEntity {

    @Column(name = "enrollment_uuid")
    @Filterable
    private UUID enrollmentUuid;

    @Column(name = "quiz_uuid")
    @Filterable
    private UUID quizUuid;

    @Column(name = "attempt_number")
    @Filterable
    private Integer attemptNumber;

    @Column(name = "started_at")
    @Filterable
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    @Filterable
    private LocalDateTime submittedAt;

    @Column(name = "time_taken_minutes")
    private Integer timeTakenMinutes;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "percentage")
    private BigDecimal percentage;

    @Column(name = "is_passed")
    @Filterable
    private Boolean isPassed;

    @Column(name = "status")
    @Convert(converter = AttemptStatusConverter.class)
    @Filterable
    private AttemptStatus status;

    @Column(name = "graded_by")
    private UUID gradedByUuid;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
}