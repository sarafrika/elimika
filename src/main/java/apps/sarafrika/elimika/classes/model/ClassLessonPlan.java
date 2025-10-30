package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "class_lesson_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassLessonPlan extends BaseEntity {

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "lesson_uuid")
    private UUID lessonUuid;

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    @Column(name = "scheduled_instance_uuid")
    private UUID scheduledInstanceUuid;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "notes")
    private String notes;
}
