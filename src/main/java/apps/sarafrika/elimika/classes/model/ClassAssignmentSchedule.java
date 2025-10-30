package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.classes.util.converter.ClassAssessmentReleaseStrategyConverter;
import apps.sarafrika.elimika.classes.util.enums.ClassAssessmentReleaseStrategy;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_assignment_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassAssignmentSchedule extends BaseEntity {

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "assignment_uuid")
    private UUID assignmentUuid;

    @Column(name = "class_lesson_plan_uuid")
    private UUID classLessonPlanUuid;

    @Column(name = "visible_at")
    private LocalDateTime visibleAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "grading_due_at")
    private LocalDateTime gradingDueAt;

    @Column(name = "timezone")
    private String timezone;

    @Convert(converter = ClassAssessmentReleaseStrategyConverter.class)
    @Column(name = "release_strategy")
    private ClassAssessmentReleaseStrategy releaseStrategy = ClassAssessmentReleaseStrategy.INHERITED;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "notes")
    private String notes;
}
