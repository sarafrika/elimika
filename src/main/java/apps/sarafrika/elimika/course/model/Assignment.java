package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "assignments")
public class Assignment extends BaseEntity {

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "max_points")
    private BigDecimal maxPoints;

    @Column(name = "rubric_uuid")
    private UUID rubricUuid;

    @Column(name = "submission_types")
    private String[] submissionTypes;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContentStatus status;

    @Column(name = "is_active")
    private Boolean active;
}