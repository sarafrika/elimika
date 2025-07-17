package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.converter.EnrollmentStatusConverter;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quizzes")
public class Quiz extends BaseEntity {

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "attempts_allowed")
    private Integer attemptsAllowed;

    @Column(name = "passing_score")
    private BigDecimal passingScore;

    @Column(name = "rubric_uuid")
    private UUID rubricUuid;

    @Column(name = "status")
    @Convert(converter = EnrollmentStatusConverter.class)
    private ContentStatus status;

    @Column(name = "active")
    private Boolean active;
}
